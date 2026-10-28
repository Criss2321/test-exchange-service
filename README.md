# Exchange Service

Microservicio que convierte montos entre monedas con el tipo de cambio vigente de [ExchangeRate-API](https://www.exchangerate-api.com/docs) y registra cada operación en una base H2 en memoria.

## Stack

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Cloud OpenFeign | 2025.1.1 |
| Spring Data JPA + H2 | En memoria |
| Resilience4j (retry y circuit breaker) | 2.3.0 |
| springdoc-openapi (Swagger UI) | 3.0.3 |
| JUnit 5, Mockito, AssertJ | Pruebas |

## Cómo ejecutar

Requisito: JDK 17 o superior. Maven no hace falta porque el proyecto incluye el wrapper.

La API key no está versionada. Se lee de la variable de entorno `EXCHANGE_RATE_API_KEY`. Usa la key que viene en el documento del reto.

**Linux / macOS / Git Bash**

```bash
export EXCHANGE_RATE_API_KEY=<tu-api-key>
./mvnw spring-boot:run
```

**PowerShell**

```powershell
$env:EXCHANGE_RATE_API_KEY="<tu-api-key>"
.\mvnw.cmd spring-boot:run
```

**IntelliJ IDEA:** en la configuración de ejecución de `ExchangeServiceApplication`, agrega `EXCHANGE_RATE_API_KEY=<tu-api-key>` en *Environment variables*.

Si la variable no está definida, la aplicación no arranca e indica qué falta.

Con la aplicación arriba:

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Consola H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:testdb`, usuario `sa`, clave `password`) |

### Pruebas

```bash
./mvnw test
```

Las pruebas no necesitan la API key ni conexión a internet.

## Endpoints

### `POST /api/conversion`

```bash
curl -X POST http://localhost:8080/api/conversion \
  -H "Content-Type: application/json" \
  -d '{"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}'
```

```json
{
  "monto": 100,
  "montoConvertido": 344.86,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN",
  "tipoCambio": 3.4486
}
```

Los códigos de moneda se aceptan en mayúsculas o minúsculas. El monto admite hasta 2 decimales y el monto convertido se redondea a 2 decimales (`HALF_UP`).

### `GET /api/conversion/historial`

Conversiones registradas, de la más reciente a la más antigua. La fecha se guarda en UTC.

```json
[
  {
    "id": "aa0e5917-f083-4395-be70-5b43d34615e9",
    "monto": 100,
    "montoConvertido": 344.86,
    "monedaOrigen": "USD",
    "monedaDestino": "PEN",
    "tipoCambio": 3.4486,
    "fechaOperacion": "2026-10-28T03:41:35.540023Z"
  }
]
```

## Errores

Los errores siguen el formato [Problem Details (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457).

| Caso | HTTP |
|---|---|
| Campos faltantes, monto ≤ 0, más de 2 decimales o código de moneda mal formado | 400 |
| JSON mal formado o con tipos incorrectos | 400 |
| Moneda de origen igual a la de destino | 400 |
| Monto tan pequeño que la conversión redondea a 0.00 | 400 |
| Moneda que ExchangeRate-API no soporta | 422 |
| ExchangeRate-API caída, lenta (timeout), con key inválida, sin cuota o con el circuito abierto | 503 |
| La base de datos no está disponible al guardar o al leer el historial | 503 |
| Error no previsto | 500, con un mensaje genérico; el detalle queda en el log |

```json
{
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "Uno o más campos no son válidos",
  "instance": "/api/conversion",
  "errores": {
    "monto": "El monto debe ser mayor a cero",
    "monedaOrigen": "Debe ser un código de moneda de 3 letras, por ejemplo USD"
  }
}
```

## Resiliencia frente a ExchangeRate-API

| Mecanismo | Configuración | Qué hace |
|---|---|---|
| Timeout | 3 s para conectar, 3 s para leer | Una API lenta no deja la petición colgada. |
| Reintento | 2 intentos, 300 ms entre ellos | Solo ante fallas de red, timeout al conectar o al esperar la respuesta, o 5xx. Un 4xx (moneda no soportada, key inválida) no se reintenta porque volvería a fallar igual. |
| Circuit breaker | Ventana de los últimos 10 intentos (cada reintento cuenta), se abre con 50 % de fallas a partir de 5 intentos y queda abierto 30 s | Con el circuito abierto se responde 503 al instante, sin llamar a la API. Pasado ese tiempo deja pasar llamadas de prueba y se cierra si responden bien. |

Los valores se ajustan en `application.yaml`, bajo `exchange-rate.api.resiliencia`. Una moneda no soportada no cuenta como falla: el proveedor respondió bien, el problema es el dato.

## Arquitectura

```text
pe.test.exchange
├── domain
│   ├── model          Conversion (agregado) y los value objects Monto, Moneda, ParMonedas, TipoCambio
│   ├── service        ConversorMoneda y el puerto ProveedorTipoCambio
│   ├── repository     ConversionRepository (puerto)
│   └── exception      Excepciones de negocio
├── application
│   ├── controller     ConversionController
│   ├── service        ConversionService
│   ├── dto            Request y responses
│   ├── mapper         ConversionMapper
│   └── handler        GlobalExceptionHandler
└── infrastructure
    ├── external       Cliente Feign y ExchangeRateApiAdapter
    ├── persistence    Entidad JPA, repositorio Spring Data y ConversionRepositoryAdapter
    └── config         Beans, propiedades, resiliencia, Jackson y OpenAPI
```

```text
ConversionController
  -> ConversionService (caso de uso)
       -> ConversorMoneda (dominio) -> ProveedorTipoCambio <- ExchangeRateApiAdapter (Feign)
       -> ConversionRepository <- ConversionRepositoryAdapter (JPA)
```

## Decisiones de diseño

**El dominio no conoce Spring, JPA ni Feign.** Separé la entidad JPA del agregado `Conversion` y definí dos puertos (`ProveedorTipoCambio` y `ConversionRepository`) que implementa la infraestructura. Así las reglas se prueban sin levantar Spring, y cambiar de proveedor de tipo de cambio significa escribir otro adaptador sin tocar el dominio.

**Las reglas viven en los value objects.** `Monto` no acepta valores ≤ 0 ni más de 2 decimales, `Moneda` normaliza y valida el código, y `ParMonedas` impide convertir una moneda a sí misma. En la API, Bean Validation repite las validaciones de forma para devolver un error por campo. La regla de negocio sigue estando en el dominio y el DTO reutiliza sus constantes.

**Los errores de ExchangeRate-API se traducen a errores del dominio.** El adaptador lee el `error-type` de la respuesta: `unsupported-code` significa que la moneda no existe (422), y cualquier otro fallo (key inválida, cuota, caída o timeout) significa que el proveedor no está disponible (503). Las excepciones no arrastran la `FeignException` original, porque su mensaje incluye la URL con la API key.

**Dinero con `BigDecimal` normalizado.** Los value objects eliminan los ceros a la derecha. Así `100` y `100.00` son el mismo monto, y la API responde igual en la conversión y en el historial.

**Las caídas de la base de datos también se traducen.** El adaptador de persistencia convierte las fallas pasajeras (conexión, transacción, recurso no disponible) en `RegistroNoDisponibleException`. Si no se pudo guardar, el cliente recibe un 503 y no un resultado que no quedó registrado. Un error que no es pasajero, como una violación de restricción, sigue como 500, porque reintentar no lo arregla.

**Retry y circuit breaker sin anotaciones.** Uso los módulos de Resilience4j directamente en el adaptador, decorando la llamada de Feign. Así se ve en un solo lugar qué se reintenta y qué cuenta como falla, y no depende de AOP.

**La fecha sale de un `Clock` inyectado y se guarda en UTC.** Las pruebas fijan el reloj, y el historial ordena bien sin depender de la zona horaria del servidor.

## Pruebas

80 pruebas:

| Grupo | Pruebas | Qué riesgo cubre |
|---|---|---|
| Dominio | 39 | Montos ≤ 0, con más de 2 decimales o demasiado grandes; códigos de moneda mal formados; misma moneda; redondeo y conversiones que redondean a cero. |
| Caso de uso (Mockito) | 5 | No se guarda nada si el proveedor falla, no se consulta al proveedor si las monedas son iguales y no se devuelve un resultado que no se pudo registrar. |
| Controller (`@WebMvcTest`) | 13 | Contrato HTTP, mensajes de validación por campo, JSON mal formado y el código de estado de cada error. |
| Adaptador de ExchangeRate-API | 15 | Traducción de cada respuesta de la API (7 unitarias) y, contra un servidor HTTP real, URL con key, 404 `unsupported-code`, 403, respuesta no JSON, timeout, reintento y apertura del circuito (8). |
| Persistencia | 6 | Guardado y lectura en H2 con `@DataJpaTest` (2) y traducción de caídas de la base y fallas al confirmar la transacción, y una violación de restricción que no se presenta como falla pasajera (4). |
| Integración | 2 | El contexto completo levanta y el flujo POST + historial devuelve el JSON exacto que recibe el cliente. |

## Con más tiempo

- Cachear las tasas por moneda base: la API las actualiza una vez al día y cada conversión gasta cuota.
- Usar los decimales propios de cada moneda (JPY no usa decimales, KWD usa 3) en lugar de 2 fijos.
- Paginar el historial.
- Métricas del circuito y de los reintentos con Actuator y Micrometer.
- Base de datos persistente con migraciones (Flyway).

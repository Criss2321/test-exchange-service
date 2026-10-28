# Exchange Service

Microservicio que convierte montos entre monedas con el tipo de cambio vigente de [ExchangeRate-API](https://www.exchangerate-api.com/docs) y registra cada operación en una base H2 en memoria.

## Stack

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Cloud OpenFeign | 2025.1.1 |
| Spring Data JPA + H2 | En memoria |
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
| ExchangeRate-API caída, lenta (timeout), con key inválida o sin cuota | 503 |
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
    └── config         Beans, propiedades, Jackson y OpenAPI
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

**La fecha sale de un `Clock` inyectado y se guarda en UTC.** Las pruebas fijan el reloj, y el historial ordena bien sin depender de la zona horaria del servidor.

## Pruebas

70 pruebas:

- **Dominio:** invariantes de cada value object, redondeo, conversión que redondea a cero e igualdad entre montos con distinta escala.
- **Caso de uso (Mockito):** no se guarda nada si el proveedor falla, y no se consulta al proveedor si las monedas son iguales.
- **Controller (`@WebMvcTest`):** contrato HTTP, mensajes de validación y código de estado de cada error.
- **Adaptador Feign contra un servidor HTTP real** (`HttpServer` del JDK): URL con key y moneda, lectura de `conversion_rates`, 404 `unsupported-code`, 403, respuesta no JSON y timeout de lectura.
- **Persistencia (`@DataJpaTest`):** guardado y lectura en H2 y orden del historial.
- **Integración:** POST y luego historial, verificando el JSON exacto que recibe el cliente.

## Con más tiempo

- Cachear las tasas por moneda base: la API las actualiza una vez al día y cada conversión gasta cuota.
- Usar los decimales propios de cada moneda (JPY no usa decimales, KWD usa 3) en lugar de 2 fijos.
- Paginar el historial.
- Circuit breaker con Resilience4j sobre el cliente del proveedor.
- Base de datos persistente con migraciones (Flyway).

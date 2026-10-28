package pe.test.exchange.application.controller;

final class EjemplosError {

    static final String CAMPOS_INVALIDOS = """
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
            """;

    static final String MONEDAS_IGUALES = """
            {
              "title": "Solicitud inválida",
              "status": 400,
              "detail": "La moneda de origen y destino deben ser distintas, ambas son 'USD'",
              "instance": "/api/conversion"
            }
            """;

    static final String MONEDA_NO_SOPORTADA = """
            {
              "title": "Moneda no soportada",
              "status": 422,
              "detail": "La moneda 'XXX' no está soportada por el proveedor de tipo de cambio",
              "instance": "/api/conversion"
            }
            """;

    static final String PROVEEDOR_NO_DISPONIBLE = """
            {
              "title": "Tipo de cambio no disponible",
              "status": 503,
              "detail": "El servicio de tipo de cambio no está disponible en este momento, intenta nuevamente más tarde",
              "instance": "/api/conversion"
            }
            """;

    private EjemplosError() {
    }
}

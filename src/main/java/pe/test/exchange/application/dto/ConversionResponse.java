package pe.test.exchange.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resultado de la conversión")
public record ConversionResponse(

        @Schema(example = "100")
        BigDecimal monto,

        @Schema(example = "344.86")
        BigDecimal montoConvertido,

        @Schema(example = "USD")
        String monedaOrigen,

        @Schema(example = "PEN")
        String monedaDestino,

        @Schema(example = "3.4486")
        BigDecimal tipoCambio
) {
}

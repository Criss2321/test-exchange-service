package pe.test.exchange.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Conversión registrada en el historial")
public record ConversionHistorialResponse(

        UUID id,

        @Schema(example = "100")
        BigDecimal monto,

        @Schema(example = "344.86")
        BigDecimal montoConvertido,

        @Schema(example = "USD")
        String monedaOrigen,

        @Schema(example = "PEN")
        String monedaDestino,

        @Schema(example = "3.4486")
        BigDecimal tipoCambio,

        @Schema(example = "2026-10-27T15:15:30Z")
        Instant fechaOperacion
) {
}

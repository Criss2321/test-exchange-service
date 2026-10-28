package pe.test.exchange.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import pe.test.exchange.domain.model.Monto;

import java.math.BigDecimal;

@Schema(description = "Datos para convertir un monto de una moneda a otra")
public record ConversionRequest(

        @Schema(description = "Monto a convertir", example = "100")
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        @Digits(integer = Monto.ENTEROS_PERMITIDOS, fraction = Monto.DECIMALES_PERMITIDOS,
                message = "El monto admite hasta {integer} enteros y {fraction} decimales")
        BigDecimal monto,

        @Schema(description = "Código ISO 4217 de la moneda de origen", example = "USD")
        @NotNull(message = "La moneda de origen es obligatoria")
        @Pattern(regexp = CODIGO_MONEDA, message = MENSAJE_CODIGO_MONEDA)
        String monedaOrigen,

        @Schema(description = "Código ISO 4217 de la moneda de destino", example = "PEN")
        @NotNull(message = "La moneda de destino es obligatoria")
        @Pattern(regexp = CODIGO_MONEDA, message = MENSAJE_CODIGO_MONEDA)
        String monedaDestino
) {

    private static final String CODIGO_MONEDA = "^\\s*[A-Za-z]{3}\\s*$";
    private static final String MENSAJE_CODIGO_MONEDA = "Debe ser un código de moneda de 3 letras, por ejemplo USD";
}

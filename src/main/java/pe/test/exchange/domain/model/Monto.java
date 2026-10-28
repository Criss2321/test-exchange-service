package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.MontoInvalidoException;

import java.math.BigDecimal;

public record Monto(BigDecimal valor) {

    public static final int ENTEROS_PERMITIDOS = 15;
    public static final int DECIMALES_PERMITIDOS = 2;

    public Monto {
        if (valor == null) {
            throw new MontoInvalidoException("El monto es obligatorio");
        }
        if (valor.signum() <= 0) {
            throw new MontoInvalidoException("El monto debe ser mayor a cero");
        }
        valor = valor.stripTrailingZeros();
        if (valor.scale() > DECIMALES_PERMITIDOS) {
            throw new MontoInvalidoException("El monto admite como máximo %d decimales".formatted(DECIMALES_PERMITIDOS));
        }
        if (valor.precision() - valor.scale() > ENTEROS_PERMITIDOS) {
            throw new MontoInvalidoException("El monto admite como máximo %d dígitos enteros".formatted(ENTEROS_PERMITIDOS));
        }
    }
}

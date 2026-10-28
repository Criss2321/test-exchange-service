package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.MontoInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record TipoCambio(ParMonedas par, BigDecimal valor) {

    public TipoCambio {
        Objects.requireNonNull(par, "El par de monedas es obligatorio");
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("El tipo de cambio debe ser mayor a cero");
        }
        valor = valor.stripTrailingZeros();
    }

    public BigDecimal aplicarA(Monto monto) {
        BigDecimal convertido = monto.valor()
                .multiply(valor)
                .setScale(Monto.DECIMALES_PERMITIDOS, RoundingMode.HALF_UP);
        if (convertido.signum() == 0) {
            throw new MontoInvalidoException("El monto es demasiado pequeño para convertirse a %s".formatted(destino().codigo()));
        }
        return convertido;
    }

    public Moneda origen() {
        return par.origen();
    }

    public Moneda destino() {
        return par.destino();
    }
}

package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.MonedasIgualesException;

import java.util.Objects;

public record ParMonedas(Moneda origen, Moneda destino) {

    public ParMonedas {
        Objects.requireNonNull(origen, "La moneda de origen es obligatoria");
        Objects.requireNonNull(destino, "La moneda de destino es obligatoria");
        if (origen.equals(destino)) {
            throw new MonedasIgualesException(origen);
        }
    }
}

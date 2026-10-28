package pe.test.exchange.domain.exception;

import pe.test.exchange.domain.model.Moneda;

public class MonedaNoSoportadaException extends DomainException {

    public MonedaNoSoportadaException(Moneda moneda) {
        super("La moneda '%s' no está soportada por el proveedor de tipo de cambio".formatted(moneda.codigo()));
    }
}

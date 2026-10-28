package pe.test.exchange.domain.exception;

import pe.test.exchange.domain.model.Moneda;

public class MonedasIgualesException extends DomainException {

    public MonedasIgualesException(Moneda moneda) {
        super("La moneda de origen y destino deben ser distintas, ambas son '%s'".formatted(moneda.codigo()));
    }
}

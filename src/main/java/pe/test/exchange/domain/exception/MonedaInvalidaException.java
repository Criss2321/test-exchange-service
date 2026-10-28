package pe.test.exchange.domain.exception;

public class MonedaInvalidaException extends DomainException {

    public MonedaInvalidaException(String codigo) {
        super("El código de moneda '%s' no es válido, debe tener 3 letras según ISO 4217".formatted(codigo));
    }
}

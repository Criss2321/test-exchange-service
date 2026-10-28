package pe.test.exchange.domain.exception;

public class RegistroNoDisponibleException extends DomainException {

    public RegistroNoDisponibleException() {
        super("No se pudo acceder al registro de conversiones, intenta nuevamente más tarde");
    }
}

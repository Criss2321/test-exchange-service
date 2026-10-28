package pe.test.exchange.domain.exception;

public class TipoCambioNoDisponibleException extends DomainException {

    public TipoCambioNoDisponibleException() {
        super("El servicio de tipo de cambio no está disponible en este momento, intenta nuevamente más tarde");
    }
}

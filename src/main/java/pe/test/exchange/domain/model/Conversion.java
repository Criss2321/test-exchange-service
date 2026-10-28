package pe.test.exchange.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Conversion {

    private final UUID id;
    private final Monto monto;
    private final TipoCambio tipoCambio;
    private final BigDecimal montoConvertido;
    private final Instant fechaOperacion;

    private Conversion(UUID id, Monto monto, TipoCambio tipoCambio, BigDecimal montoConvertido, Instant fechaOperacion) {
        this.id = Objects.requireNonNull(id);
        this.monto = Objects.requireNonNull(monto);
        this.tipoCambio = Objects.requireNonNull(tipoCambio);
        this.montoConvertido = Objects.requireNonNull(montoConvertido);
        this.fechaOperacion = Objects.requireNonNull(fechaOperacion);
    }

    public static Conversion realizar(Monto monto, TipoCambio tipoCambio, Instant fechaOperacion) {
        return new Conversion(UUID.randomUUID(), monto, tipoCambio, tipoCambio.aplicarA(monto), fechaOperacion);
    }

    public static Conversion reconstituir(UUID id, Monto monto, TipoCambio tipoCambio,
                                          BigDecimal montoConvertido, Instant fechaOperacion) {
        return new Conversion(id, monto, tipoCambio, montoConvertido, fechaOperacion);
    }

    public UUID getId() {
        return id;
    }

    public Monto getMonto() {
        return monto;
    }

    public Moneda getMonedaOrigen() {
        return tipoCambio.origen();
    }

    public Moneda getMonedaDestino() {
        return tipoCambio.destino();
    }

    public TipoCambio getTipoCambio() {
        return tipoCambio;
    }

    public BigDecimal getMontoConvertido() {
        return montoConvertido;
    }

    public Instant getFechaOperacion() {
        return fechaOperacion;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Conversion conversion && id.equals(conversion.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

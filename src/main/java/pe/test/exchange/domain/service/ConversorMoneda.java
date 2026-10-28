package pe.test.exchange.domain.service;

import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;

import java.time.Clock;
import java.time.Instant;

public class ConversorMoneda {

    private final ProveedorTipoCambio proveedorTipoCambio;
    private final Clock reloj;

    public ConversorMoneda(ProveedorTipoCambio proveedorTipoCambio, Clock reloj) {
        this.proveedorTipoCambio = proveedorTipoCambio;
        this.reloj = reloj;
    }

    public Conversion convertir(Monto monto, ParMonedas par) {
        TipoCambio tipoCambio = proveedorTipoCambio.obtenerTipoCambio(par);
        return Conversion.realizar(monto, tipoCambio, Instant.now(reloj));
    }
}

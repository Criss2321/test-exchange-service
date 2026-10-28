package pe.test.exchange.domain.service;

import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;

public interface ProveedorTipoCambio {

    TipoCambio obtenerTipoCambio(ParMonedas par);
}

package pe.test.exchange.domain.service;

import org.junit.jupiter.api.Test;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ConversorMonedaTest {

    private static final Instant AHORA = Instant.parse("2026-10-27T15:00:00Z");
    private static final ParMonedas USD_PEN = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));

    @Test
    void convierteConElTipoCambioDelProveedorYRegistraElMomentoDeLaOperacion() {
        ProveedorTipoCambio proveedor = par -> new TipoCambio(par, new BigDecimal("3.70"));
        ConversorMoneda conversor = new ConversorMoneda(proveedor, Clock.fixed(AHORA, ZoneOffset.UTC));

        Conversion conversion = conversor.convertir(new Monto(new BigDecimal("100")), USD_PEN);

        assertThat(conversion.getMontoConvertido()).isEqualTo(new BigDecimal("370.00"));
        assertThat(conversion.getTipoCambio().par()).isEqualTo(USD_PEN);
        assertThat(conversion.getFechaOperacion()).isEqualTo(AHORA);
    }
}

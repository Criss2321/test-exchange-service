package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.MontoInvalidoException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TipoCambioTest {

    private static final ParMonedas USD_PEN = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));

    @ParameterizedTest
    @CsvSource({
            "100,  3.70,    370.00",
            "100,  3.4486,  344.86",
            "10,   0.12345, 1.23",
            "1,    1.005,   1.01",
            "0.01, 0.5,     0.01"
    })
    void aplicaElTipoCambioRedondeandoADosDecimales(String monto, String tasa, String esperado) {
        TipoCambio tipoCambio = new TipoCambio(USD_PEN, new BigDecimal(tasa));

        BigDecimal convertido = tipoCambio.aplicarA(new Monto(new BigDecimal(monto)));

        assertThat(convertido).isEqualTo(new BigDecimal(esperado));
    }

    @Test
    void rechazaUnaConversionQueRedondeaACero() {
        TipoCambio tipoCambio = new TipoCambio(USD_PEN, new BigDecimal("0.0001"));
        Monto centavo = new Monto(new BigDecimal("0.01"));

        assertThatThrownBy(() -> tipoCambio.aplicarA(centavo))
                .isInstanceOf(MontoInvalidoException.class)
                .hasMessageContaining("PEN");
    }

    @Test
    void dosTiposDeCambioConElMismoValorSonIguales() {
        assertThat(new TipoCambio(USD_PEN, new BigDecimal("3.70")))
                .isEqualTo(new TipoCambio(USD_PEN, new BigDecimal("3.700000000000")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-3.70"})
    void rechazaTiposDeCambioNoPositivos(BigDecimal valor) {
        assertThatThrownBy(() -> new TipoCambio(USD_PEN, valor))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

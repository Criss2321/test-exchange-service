package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.MonedaInvalidaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonedaTest {

    @Test
    void normalizaElCodigoAMayusculasSinEspacios() {
        assertThat(new Moneda(" usd ").codigo()).isEqualTo("USD");
    }

    @Test
    void dosMonedasConElMismoCodigoSonIguales() {
        assertThat(new Moneda("pen")).isEqualTo(new Moneda("PEN"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "US", "USDD", "U5D", "U$D"})
    void rechazaCodigosQueNoCumplenElFormatoIso(String codigo) {
        assertThatThrownBy(() -> new Moneda(codigo))
                .isInstanceOf(MonedaInvalidaException.class);
    }
}

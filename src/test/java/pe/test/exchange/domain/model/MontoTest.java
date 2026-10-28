package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.MontoInvalidoException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MontoTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "100", "100.5", "100.50", "100.500", "999999999999999.99"})
    void aceptaMontosPositivosConHastaDosDecimales(String valor) {
        assertThat(new Monto(new BigDecimal(valor)).valor()).isEqualByComparingTo(valor);
    }

    @Test
    void dosMontosConElMismoValorSonIgualesSinImportarLosCerosALaDerecha() {
        assertThat(new Monto(new BigDecimal("100"))).isEqualTo(new Monto(new BigDecimal("100.00")));
    }

    @Test
    void rechazaMontoNulo() {
        assertThatThrownBy(() -> new Monto(null))
                .isInstanceOf(MontoInvalidoException.class)
                .hasMessage("El monto es obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    void rechazaMontosCeroONegativos(String valor) {
        assertThatThrownBy(() -> new Monto(new BigDecimal(valor)))
                .isInstanceOf(MontoInvalidoException.class)
                .hasMessage("El monto debe ser mayor a cero");
    }

    @Test
    void rechazaMontosConMasDeDosDecimales() {
        assertThatThrownBy(() -> new Monto(new BigDecimal("10.123")))
                .isInstanceOf(MontoInvalidoException.class)
                .hasMessage("El monto admite como máximo 2 decimales");
    }

    @Test
    void rechazaMontosConMasDeQuinceDigitosEnteros() {
        assertThatThrownBy(() -> new Monto(new BigDecimal("1000000000000000")))
                .isInstanceOf(MontoInvalidoException.class)
                .hasMessage("El monto admite como máximo 15 dígitos enteros");
    }
}

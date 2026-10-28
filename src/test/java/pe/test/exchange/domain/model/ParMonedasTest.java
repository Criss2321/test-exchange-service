package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import pe.test.exchange.domain.exception.MonedasIgualesException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParMonedasTest {

    @Test
    void creaElParCuandoLasMonedasSonDistintas() {
        ParMonedas par = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));

        assertThat(par.origen().codigo()).isEqualTo("USD");
        assertThat(par.destino().codigo()).isEqualTo("PEN");
    }

    @Test
    void rechazaMonedasIgualesAunqueDifieranEnMayusculas() {
        assertThatThrownBy(() -> new ParMonedas(new Moneda("USD"), new Moneda("usd")))
                .isInstanceOf(MonedasIgualesException.class)
                .hasMessageContaining("USD");
    }
}

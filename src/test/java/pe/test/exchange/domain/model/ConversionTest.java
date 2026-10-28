package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionTest {

    private static final Instant FECHA = Instant.parse("2026-10-27T15:30:00Z");
    private static final TipoCambio USD_PEN = new TipoCambio(
            new ParMonedas(new Moneda("USD"), new Moneda("PEN")), new BigDecimal("3.70"));

    @Test
    void realizarCalculaElMontoConvertidoYRegistraLosDatosDeLaOperacion() {
        Conversion conversion = Conversion.realizar(new Monto(new BigDecimal("100")), USD_PEN, FECHA);

        assertThat(conversion.getId()).isNotNull();
        assertThat(conversion.getMonto().valor()).isEqualByComparingTo("100");
        assertThat(conversion.getMonedaOrigen()).isEqualTo(new Moneda("USD"));
        assertThat(conversion.getMonedaDestino()).isEqualTo(new Moneda("PEN"));
        assertThat(conversion.getTipoCambio().valor()).isEqualByComparingTo("3.70");
        assertThat(conversion.getMontoConvertido()).isEqualTo(new BigDecimal("370.00"));
        assertThat(conversion.getFechaOperacion()).isEqualTo(FECHA);
    }

    @Test
    void cadaConversionTieneSuPropiaIdentidad() {
        Monto monto = new Monto(BigDecimal.TEN);

        Conversion primera = Conversion.realizar(monto, USD_PEN, FECHA);
        Conversion segunda = Conversion.realizar(monto, USD_PEN, FECHA);

        assertThat(primera).isNotEqualTo(segunda);
    }

    @Test
    void unaConversionReconstituidaEsIgualALaOriginal() {
        Conversion original = Conversion.realizar(new Monto(BigDecimal.TEN), USD_PEN, FECHA);

        Conversion reconstituida = Conversion.reconstituir(original.getId(), original.getMonto(),
                original.getTipoCambio(), original.getMontoConvertido(), original.getFechaOperacion());

        assertThat(reconstituida).isEqualTo(original).hasSameHashCodeAs(original);
    }
}

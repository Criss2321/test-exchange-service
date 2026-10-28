package pe.test.exchange.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ConversionRepositoryAdapter.class, ConversionEntityMapper.class})
class ConversionRepositoryAdapterTest {

    @Autowired
    private ConversionRepositoryAdapter repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardaLaConversionConTodosSusDatos() {
        Conversion conversion = conversion("USD", "PEN", "100", "3.4486", "2026-10-27T15:00:00Z");

        repository.guardar(conversion);
        limpiarContextoDePersistencia();

        Conversion recuperada = repository.listarHistorial().get(0);
        assertThat(recuperada).isEqualTo(conversion);
        assertThat(recuperada.getMonto()).isEqualTo(conversion.getMonto());
        assertThat(recuperada.getTipoCambio()).isEqualTo(conversion.getTipoCambio());
        assertThat(recuperada.getMontoConvertido()).isEqualTo(new BigDecimal("344.86"));
        assertThat(recuperada.getFechaOperacion()).isEqualTo(conversion.getFechaOperacion());
    }

    @Test
    void listaElHistorialDeLaMasRecienteALaMasAntigua() {
        Conversion antigua = conversion("USD", "PEN", "10", "3.70", "2026-10-25T14:00:00Z");
        Conversion reciente = conversion("EUR", "USD", "20", "1.08", "2026-10-27T23:00:00Z");
        Conversion intermedia = conversion("PEN", "EUR", "30", "0.26", "2026-10-26T17:00:00Z");
        List.of(antigua, reciente, intermedia).forEach(repository::guardar);
        limpiarContextoDePersistencia();

        assertThat(repository.listarHistorial()).containsExactly(reciente, intermedia, antigua);
    }

    private void limpiarContextoDePersistencia() {
        entityManager.flush();
        entityManager.clear();
    }

    private Conversion conversion(String origen, String destino, String monto, String tasa, String fecha) {
        ParMonedas par = new ParMonedas(new Moneda(origen), new Moneda(destino));
        return Conversion.realizar(new Monto(new BigDecimal(monto)), new TipoCambio(par, new BigDecimal(tasa)),
                Instant.parse(fecha));
    }
}

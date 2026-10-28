package pe.test.exchange.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;
import pe.test.exchange.domain.exception.RegistroNoDisponibleException;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionRepositoryAdapterErroresTest {

    private static final ParMonedas USD_PEN = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));

    @Mock
    private ConversionJpaRepository conversionJpaRepository;

    private ConversionRepositoryAdapter repository;

    @BeforeEach
    void setUp() {
        repository = new ConversionRepositoryAdapter(conversionJpaRepository, new ConversionEntityMapper());
    }

    @Test
    void guardarTraduceUnaCaidaDeLaBaseDeDatosARegistroNoDisponible() {
        when(conversionJpaRepository.save(any())).thenThrow(new DataAccessResourceFailureException("conexión rechazada"));

        assertThatThrownBy(() -> repository.guardar(conversion()))
                .isInstanceOf(RegistroNoDisponibleException.class)
                .hasNoCause();
    }

    @Test
    void guardarNoPresentaUnaViolacionDeRestriccionComoUnaFallaPasajera() {
        when(conversionJpaRepository.save(any())).thenThrow(new DataIntegrityViolationException("clave duplicada"));

        assertThatThrownBy(() -> repository.guardar(conversion()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardarTraduceUnaFallaAlConfirmarLaTransaccionARegistroNoDisponible() {
        when(conversionJpaRepository.save(any())).thenThrow(new TransactionSystemException("commit fallido"));

        assertThatThrownBy(() -> repository.guardar(conversion()))
                .isInstanceOf(RegistroNoDisponibleException.class);
    }

    @Test
    void listarHistorialTraduceUnaCaidaDeLaBaseDeDatosARegistroNoDisponible() {
        when(conversionJpaRepository.findAllByOrderByFechaOperacionDesc())
                .thenThrow(new DataAccessResourceFailureException("conexión rechazada"));

        assertThatThrownBy(() -> repository.listarHistorial())
                .isInstanceOf(RegistroNoDisponibleException.class);
    }

    private Conversion conversion() {
        return Conversion.realizar(new Monto(BigDecimal.TEN), new TipoCambio(USD_PEN, new BigDecimal("3.70")),
                Instant.parse("2026-10-27T15:00:00Z"));
    }
}

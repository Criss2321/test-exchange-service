package pe.test.exchange.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.application.dto.ConversionHistorialResponse;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.mapper.ConversionMapper;
import pe.test.exchange.domain.exception.MonedasIgualesException;
import pe.test.exchange.domain.exception.TipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ConversorMoneda;
import pe.test.exchange.domain.service.ProveedorTipoCambio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-27T15:00:00Z");
    private static final ParMonedas USD_PEN = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));

    @Mock
    private ProveedorTipoCambio proveedorTipoCambio;

    @Mock
    private ConversionRepository conversionRepository;

    private ConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new ConversionService(
                new ConversorMoneda(proveedorTipoCambio, Clock.fixed(AHORA, ZoneOffset.UTC)),
                conversionRepository,
                new ConversionMapper()
        );
    }

    @Test
    void convierteElMontoYGuardaLaOperacion() {
        when(proveedorTipoCambio.obtenerTipoCambio(USD_PEN)).thenReturn(new TipoCambio(USD_PEN, new BigDecimal("3.70")));

        ConversionResponse response = conversionService.convertir(
                new ConversionRequest(new BigDecimal("100"), "usd", "pen"));

        assertThat(response.monto()).isEqualByComparingTo("100");
        assertThat(response.montoConvertido()).isEqualTo(new BigDecimal("370.00"));
        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
        assertThat(response.tipoCambio()).isEqualByComparingTo("3.70");

        ArgumentCaptor<Conversion> guardada = ArgumentCaptor.forClass(Conversion.class);
        verify(conversionRepository).guardar(guardada.capture());
        assertThat(guardada.getValue().getMontoConvertido()).isEqualTo(new BigDecimal("370.00"));
        assertThat(guardada.getValue().getFechaOperacion()).isEqualTo(AHORA);
    }

    @Test
    void noConsultaAlProveedorCuandoLasMonedasSonIguales() {
        ConversionRequest request = new ConversionRequest(BigDecimal.TEN, "USD", "USD");

        assertThatThrownBy(() -> conversionService.convertir(request))
                .isInstanceOf(MonedasIgualesException.class);

        verifyNoInteractions(proveedorTipoCambio, conversionRepository);
    }

    @Test
    void noGuardaNadaCuandoElProveedorNoEstaDisponible() {
        when(proveedorTipoCambio.obtenerTipoCambio(any())).thenThrow(new TipoCambioNoDisponibleException());
        ConversionRequest request = new ConversionRequest(BigDecimal.TEN, "USD", "PEN");

        assertThatThrownBy(() -> conversionService.convertir(request))
                .isInstanceOf(TipoCambioNoDisponibleException.class);

        verifyNoInteractions(conversionRepository);
    }

    @Test
    void listaElHistorialEnElOrdenQueEntregaElRepositorio() {
        Conversion reciente = Conversion.realizar(new Monto(BigDecimal.ONE),
                new TipoCambio(USD_PEN, new BigDecimal("3.70")), AHORA);
        Conversion antigua = Conversion.realizar(new Monto(BigDecimal.TEN),
                new TipoCambio(USD_PEN, new BigDecimal("3.65")), AHORA.minusSeconds(86_400));
        when(conversionRepository.listarHistorial()).thenReturn(List.of(reciente, antigua));

        List<ConversionHistorialResponse> historial = conversionService.listarHistorial();

        assertThat(historial).extracting(ConversionHistorialResponse::id)
                .containsExactly(reciente.getId(), antigua.getId());
        assertThat(historial.get(1).tipoCambio()).isEqualByComparingTo("3.65");
    }
}

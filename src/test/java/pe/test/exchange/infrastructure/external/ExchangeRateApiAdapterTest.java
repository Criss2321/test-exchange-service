package pe.test.exchange.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.exception.MonedaNoSoportadaException;
import pe.test.exchange.domain.exception.TipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.infrastructure.config.ExchangeRateApiProperties;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateApiAdapterTest {

    private static final String API_KEY = "clave-de-prueba";
    private static final ParMonedas USD_PEN = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    private ExchangeRateApiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ExchangeRateApiAdapter(exchangeRateApiClient, new ExchangeRateApiProperties(API_KEY),
                JsonMapper.builder().build());
    }

    @Test
    void obtieneLaTasaDeLaMonedaDestinoParaLaMonedaOrigen() {
        responderConTasas(Map.of("USD", BigDecimal.ONE, "PEN", new BigDecimal("3.4486")));

        TipoCambio tipoCambio = adapter.obtenerTipoCambio(USD_PEN);

        assertThat(tipoCambio.par()).isEqualTo(USD_PEN);
        assertThat(tipoCambio.valor()).isEqualByComparingTo("3.4486");
    }

    @Test
    void lanzaMonedaNoSoportadaCuandoElDestinoNoFiguraEnLasTasas() {
        responderConTasas(Map.of("USD", BigDecimal.ONE));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(MonedaNoSoportadaException.class)
                .hasMessageContaining("PEN");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3.70"})
    void lanzaTipoCambioNoDisponibleCuandoLaTasaRecibidaNoEsPositiva(String tasa) {
        responderConTasas(Map.of("PEN", new BigDecimal(tasa)));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
    }

    @Test
    void lanzaMonedaNoSoportadaCuandoLaRespuestaInformaUnsupportedCode() {
        when(exchangeRateApiClient.getLatestRates(API_KEY, "USD"))
                .thenReturn(new ExchangeRateApiResponse("error", null, null, "unsupported-code"));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(MonedaNoSoportadaException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void lanzaTipoCambioNoDisponibleCuandoLaRespuestaInformaOtroError() {
        when(exchangeRateApiClient.getLatestRates(API_KEY, "USD"))
                .thenReturn(new ExchangeRateApiResponse("error", null, null, "quota-reached"));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
    }

    @Test
    void lanzaTipoCambioNoDisponibleCuandoLaRespuestaLlegaVacia() {
        when(exchangeRateApiClient.getLatestRates(API_KEY, "USD")).thenReturn(null);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
    }

    private void responderConTasas(Map<String, BigDecimal> tasas) {
        when(exchangeRateApiClient.getLatestRates(API_KEY, "USD"))
                .thenReturn(new ExchangeRateApiResponse("success", "USD", tasas, null));
    }
}

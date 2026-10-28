package pe.test.exchange.infrastructure.external;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.test.exchange.domain.exception.MonedaNoSoportadaException;
import pe.test.exchange.domain.exception.TipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.infrastructure.config.ResilienciaProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ExchangeRateApiAdapterHttpTest {

    private static final String API_KEY = "clave-de-prueba";
    private static final int TIMEOUT_LECTURA_MS = 500;
    private static final ParMonedas USD_PEN = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));
    private static final RespuestaSimulada TASAS_USD = new RespuestaSimulada(200, """
            {"result":"success","base_code":"USD","conversion_rates":{"USD":1,"PEN":3.4486,"EUR":0.9243}}
            """, 0);
    private static final RespuestaSimulada ERROR_DEL_SERVIDOR = new RespuestaSimulada(503, "Service Unavailable", 0);
    private static final RespuestaSimulada MONEDA_NO_SOPORTADA = new RespuestaSimulada(404, """
            {"result":"error","documentation":"https://www.exchangerate-api.com/docs","error-type":"unsupported-code"}
            """, 0);

    private static final Deque<RespuestaSimulada> respuestas = new ConcurrentLinkedDeque<>();
    private static final AtomicInteger llamadasRecibidas = new AtomicInteger();
    private static HttpServer servidor;
    private static volatile String rutaRecibida;

    @Autowired
    private ExchangeRateApiAdapter adapter;

    @Autowired
    private CircuitBreaker circuitBreaker;

    @Autowired
    private ResilienciaProperties resiliencia;

    @DynamicPropertySource
    static void apuntarAlServidorSimulado(DynamicPropertyRegistry registry) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", ExchangeRateApiAdapterHttpTest::responder);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
        registry.add("exchange-rate.api.base-url", () -> "http://localhost:" + servidor.getAddress().getPort());
        registry.add("exchange-rate.api.key", () -> API_KEY);
        registry.add("exchange-rate.api.resiliencia.reintento.espera", () -> "10ms");
        registry.add("spring.cloud.openfeign.client.config.exchangeRateApi.read-timeout", () -> TIMEOUT_LECTURA_MS);
    }

    @AfterAll
    static void detenerServidor() {
        servidor.stop(0);
    }

    @BeforeEach
    void reiniciar() {
        respuestas.clear();
        llamadasRecibidas.set(0);
        circuitBreaker.reset();
    }

    @Test
    void consultaLaApiConLaKeyYLaMonedaOrigenYLeeLasTasas() {
        responderCon(TASAS_USD);

        TipoCambio tipoCambio = adapter.obtenerTipoCambio(USD_PEN);

        assertThat(rutaRecibida).isEqualTo("/" + API_KEY + "/latest/USD");
        assertThat(tipoCambio.valor()).isEqualByComparingTo("3.4486");
    }

    @Test
    void traduceUnsupportedCodeAMonedaNoSoportadaSinReintentar() {
        responderCon(MONEDA_NO_SOPORTADA);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(MonedaNoSoportadaException.class);
        assertThat(llamadasRecibidas).hasValue(1);
    }

    @Test
    void traduceUnaKeyInvalidaATipoCambioNoDisponibleSinReintentar() {
        responderCon(new RespuestaSimulada(403, """
                {"result":"error","error-type":"invalid-key"}
                """, 0));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class)
                .hasNoCause()
                .message().doesNotContain(API_KEY);
        assertThat(llamadasRecibidas).hasValue(1);
    }

    @Test
    void reintentaCuandoLaApiFallaPorUnErrorDelServidorYSeRecupera() {
        responderCon(ERROR_DEL_SERVIDOR, TASAS_USD);

        TipoCambio tipoCambio = adapter.obtenerTipoCambio(USD_PEN);

        assertThat(tipoCambio.valor()).isEqualByComparingTo("3.4486");
        assertThat(llamadasRecibidas).hasValue(2);
    }

    @Test
    void traduceUnErrorSinCuerpoJsonATipoCambioNoDisponibleTrasAgotarLosReintentos() {
        responderCon(new RespuestaSimulada(502, "Bad Gateway", 0));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
        assertThat(llamadasRecibidas).hasValue(resiliencia.reintento().maxIntentos());
    }

    @Test
    void cortaLaEsperaYReintentaCuandoLaApiSuperaElTimeoutDeLectura() {
        responderCon(new RespuestaSimulada(200, TASAS_USD.cuerpo(), TIMEOUT_LECTURA_MS * 3));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
        assertThat(llamadasRecibidas).hasValue(resiliencia.reintento().maxIntentos());
    }

    @Test
    void abreElCircuitoTrasFallasRepetidasYDejaDeConsultarLaApi() {
        responderCon(ERROR_DEL_SERVIDOR);
        int peticionesFallidas = 0;
        while (circuitBreaker.getState() == CircuitBreaker.State.CLOSED
                && peticionesFallidas < resiliencia.circuito().ventana()) {
            assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                    .isInstanceOf(TipoCambioNoDisponibleException.class);
            peticionesFallidas++;
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        int llamadasHastaAbrirse = llamadasRecibidas.get();

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
        assertThat(llamadasRecibidas).hasValue(llamadasHastaAbrirse);
    }

    @Test
    void unaMonedaNoSoportadaNoCuentaComoFallaDelProveedor() {
        responderCon(MONEDA_NO_SOPORTADA);
        for (int i = 0; i < resiliencia.circuito().ventana(); i++) {
            assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                    .isInstanceOf(MonedaNoSoportadaException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private static void responderCon(RespuestaSimulada... secuencia) {
        respuestas.addAll(List.of(secuencia));
    }

    private static void responder(HttpExchange intercambio) throws IOException {
        llamadasRecibidas.incrementAndGet();
        rutaRecibida = intercambio.getRequestURI().getPath();
        RespuestaSimulada respuesta = respuestas.size() > 1 ? respuestas.poll() : respuestas.peek();
        esperar(respuesta.demoraMs());
        byte[] cuerpo = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(respuesta.status(), cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record RespuestaSimulada(int status, String cuerpo, long demoraMs) {
    }
}

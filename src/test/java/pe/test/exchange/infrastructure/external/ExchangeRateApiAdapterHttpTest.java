package pe.test.exchange.infrastructure.external;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ExchangeRateApiAdapterHttpTest {

    private static final String API_KEY = "clave-de-prueba";
    private static final int TIMEOUT_LECTURA_MS = 500;
    private static final ParMonedas USD_PEN = new ParMonedas(new Moneda("USD"), new Moneda("PEN"));

    private static HttpServer servidor;
    private static volatile RespuestaSimulada respuestaSimulada;
    private static volatile String rutaRecibida;

    @Autowired
    private ExchangeRateApiAdapter adapter;

    @DynamicPropertySource
    static void apuntarAlServidorSimulado(DynamicPropertyRegistry registry) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", ExchangeRateApiAdapterHttpTest::responder);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
        registry.add("exchange-rate.api.base-url", () -> "http://localhost:" + servidor.getAddress().getPort());
        registry.add("exchange-rate.api.key", () -> API_KEY);
        registry.add("spring.cloud.openfeign.client.config.exchangeRateApi.read-timeout", () -> TIMEOUT_LECTURA_MS);
    }

    @AfterAll
    static void detenerServidor() {
        servidor.stop(0);
    }

    @Test
    void consultaLaApiConLaKeyYLaMonedaOrigenYLeeLasTasas() {
        respuestaSimulada = new RespuestaSimulada(200, """
                {"result":"success","base_code":"USD","conversion_rates":{"USD":1,"PEN":3.4486,"EUR":0.9243}}
                """, 0);

        TipoCambio tipoCambio = adapter.obtenerTipoCambio(USD_PEN);

        assertThat(rutaRecibida).isEqualTo("/" + API_KEY + "/latest/USD");
        assertThat(tipoCambio.valor()).isEqualByComparingTo("3.4486");
    }

    @Test
    void traduceUnsupportedCodeAMonedaNoSoportada() {
        respuestaSimulada = new RespuestaSimulada(404, """
                {"result":"error","documentation":"https://www.exchangerate-api.com/docs","error-type":"unsupported-code"}
                """, 0);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(MonedaNoSoportadaException.class);
    }

    @Test
    void traduceUnaKeyInvalidaATipoCambioNoDisponible() {
        respuestaSimulada = new RespuestaSimulada(403, """
                {"result":"error","error-type":"invalid-key"}
                """, 0);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class)
                .hasNoCause()
                .message().doesNotContain(API_KEY);
    }

    @Test
    void traduceUnErrorSinCuerpoJsonATipoCambioNoDisponible() {
        respuestaSimulada = new RespuestaSimulada(502, "Bad Gateway", 0);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
    }

    @Test
    void cortaLaEsperaCuandoLaApiSuperaElTimeoutDeLectura() {
        respuestaSimulada = new RespuestaSimulada(200, """
                {"result":"success","base_code":"USD","conversion_rates":{"PEN":3.4486}}
                """, TIMEOUT_LECTURA_MS * 3);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD_PEN))
                .isInstanceOf(TipoCambioNoDisponibleException.class);
    }

    private static void responder(HttpExchange intercambio) throws IOException {
        rutaRecibida = intercambio.getRequestURI().getPath();
        RespuestaSimulada respuesta = respuestaSimulada;
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

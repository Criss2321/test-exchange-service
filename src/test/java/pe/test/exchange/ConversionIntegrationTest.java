package pe.test.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.test.exchange.infrastructure.external.ExchangeRateApiClient;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "exchange-rate.api.key=clave-de-prueba")
@AutoConfigureMockMvc
@Transactional
class ConversionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateApiClient exchangeRateApiClient;

    @Test
    void convierteConsultandoElProveedorYRegistraLaOperacionEnElHistorial() throws Exception {
        when(exchangeRateApiClient.getLatestRates(anyString(), eq("USD"))).thenReturn(new ExchangeRateApiResponse(
                "success", "USD", Map.of("USD", BigDecimal.ONE, "PEN", new BigDecimal("3.70")), null));

        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 100.00, "monedaOrigen": "usd", "monedaDestino": "pen"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"monto":100,"montoConvertido":370.00,"monedaOrigen":"USD","monedaDestino":"PEN","tipoCambio":3.7}
                        """))
                .andExpect(content().string(containsString("\"monto\":100,")))
                .andExpect(content().string(containsString("\"montoConvertido\":370.00")));

        mockMvc.perform(get("/api/conversion/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].monedaOrigen").value("USD"))
                .andExpect(jsonPath("$[0].monedaDestino").value("PEN"))
                .andExpect(content().string(containsString("\"monto\":100,")))
                .andExpect(content().string(containsString("\"montoConvertido\":370.00")))
                .andExpect(jsonPath("$[0].fechaOperacion").exists());
    }
}

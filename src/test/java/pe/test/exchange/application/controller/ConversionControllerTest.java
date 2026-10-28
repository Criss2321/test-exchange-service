package pe.test.exchange.application.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.test.exchange.application.dto.ConversionHistorialResponse;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConversionService;
import pe.test.exchange.domain.exception.MonedaNoSoportadaException;
import pe.test.exchange.domain.exception.MonedasIgualesException;
import pe.test.exchange.domain.exception.RegistroNoDisponibleException;
import pe.test.exchange.domain.exception.TipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Moneda;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
class ConversionControllerTest {

    private static final String URL = "/api/conversion";
    private static final String SOLICITUD_VALIDA = """
            {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}
            """;
    private static final String MENSAJE_CODIGO_MONEDA = "Debe ser un código de moneda de 3 letras, por ejemplo USD";
    private static final String MENSAJE_CUERPO_INVALIDO =
            "El cuerpo de la solicitud no es un JSON válido o tiene tipos de datos incorrectos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversionService conversionService;

    @Test
    void convertirDevuelveElResultadoDeLaConversion() throws Exception {
        when(conversionService.convertir(any())).thenReturn(new ConversionResponse(
                new BigDecimal("100"), new BigDecimal("370.00"), "USD", "PEN", new BigDecimal("3.70")));

        convertir(SOLICITUD_VALIDA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100))
                .andExpect(jsonPath("$.montoConvertido").value(370.00))
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"))
                .andExpect(jsonPath("$.tipoCambio").value(3.70));
    }

    @Test
    void convertirRechazaCamposInvalidosIndicandoCadaError() throws Exception {
        convertir("""
                {"monto": -5, "monedaOrigen": "US", "monedaDestino": ""}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errores.monto").value("El monto debe ser mayor a cero"))
                .andExpect(jsonPath("$.errores.monedaOrigen").value(MENSAJE_CODIGO_MONEDA))
                .andExpect(jsonPath("$.errores.monedaDestino").value(MENSAJE_CODIGO_MONEDA));

        verifyNoInteractions(conversionService);
    }

    @Test
    void convertirIndicaLosCamposObligatoriosQueFaltan() throws Exception {
        convertir("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto es obligatorio"))
                .andExpect(jsonPath("$.errores.monedaOrigen").value("La moneda de origen es obligatoria"))
                .andExpect(jsonPath("$.errores.monedaDestino").value("La moneda de destino es obligatoria"));
    }

    @Test
    void convertirRechazaMontosConMasDeDosDecimales() throws Exception {
        convertir("""
                {"monto": 10.123, "monedaOrigen": "USD", "monedaDestino": "PEN"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto admite hasta 15 enteros y 2 decimales"));
    }

    @Test
    void convertirRechazaUnCuerpoQueNoEsJsonValido() throws Exception {
        convertir("{\"monto\": 100, \"monedaOrigen\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_CUERPO_INVALIDO));
    }

    @Test
    void convertirRechazaUnMontoQueNoEsNumerico() throws Exception {
        convertir("""
                {"monto": "cien", "monedaOrigen": "USD", "monedaDestino": "PEN"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_CUERPO_INVALIDO));
    }

    @Test
    void convertirResponde400CuandoLasMonedasSonIguales() throws Exception {
        when(conversionService.convertir(any())).thenThrow(new MonedasIgualesException(new Moneda("USD")));

        convertir(SOLICITUD_VALIDA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("deben ser distintas")));
    }

    @Test
    void convertirResponde422CuandoLaMonedaNoEsSoportada() throws Exception {
        when(conversionService.convertir(any())).thenThrow(new MonedaNoSoportadaException(new Moneda("XYZ")));

        convertir(SOLICITUD_VALIDA)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Moneda no soportada"))
                .andExpect(jsonPath("$.detail", containsString("XYZ")));
    }

    @Test
    void convertirResponde503CuandoElProveedorNoEstaDisponible() throws Exception {
        when(conversionService.convertir(any())).thenThrow(new TipoCambioNoDisponibleException());

        convertir(SOLICITUD_VALIDA)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Tipo de cambio no disponible"));
    }

    @Test
    void convertirResponde503CuandoLaConversionNoSePudoRegistrar() throws Exception {
        when(conversionService.convertir(any())).thenThrow(new RegistroNoDisponibleException());

        convertir(SOLICITUD_VALIDA)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Registro no disponible"));
    }

    @Test
    void convertirNoExponeDetallesInternosAnteUnErrorInesperado() throws Exception {
        when(conversionService.convertir(any())).thenThrow(new IllegalStateException("conexión a BD perdida"));

        convertir(SOLICITUD_VALIDA)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado, intenta nuevamente más tarde"));
    }

    @Test
    void listarHistorialDevuelveLasConversionesRegistradas() throws Exception {
        UUID id = UUID.randomUUID();
        when(conversionService.listarHistorial()).thenReturn(List.of(new ConversionHistorialResponse(
                id, new BigDecimal("100.00"), new BigDecimal("370.00"), "USD", "PEN",
                new BigDecimal("3.70"), Instant.parse("2026-10-27T15:15:30Z"))));

        mockMvc.perform(get(URL + "/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].fechaOperacion").value("2026-10-27T15:15:30Z"));
    }

    @Test
    void listarHistorialResponde503CuandoElRegistroNoEstaDisponible() throws Exception {
        when(conversionService.listarHistorial()).thenThrow(new RegistroNoDisponibleException());

        mockMvc.perform(get(URL + "/historial"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Registro no disponible"));
    }

    private ResultActions convertir(String cuerpo) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}

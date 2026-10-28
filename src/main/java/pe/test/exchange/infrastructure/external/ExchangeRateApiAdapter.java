package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.exception.MonedaNoSoportadaException;
import pe.test.exchange.domain.exception.TipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.domain.service.ProveedorTipoCambio;
import pe.test.exchange.infrastructure.config.ExchangeRateApiProperties;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiAdapter implements ProveedorTipoCambio {

    private static final String RESULTADO_EXITOSO = "success";
    private static final String ERROR_MONEDA_NO_SOPORTADA = "unsupported-code";

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final ExchangeRateApiProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    public TipoCambio obtenerTipoCambio(ParMonedas par) {
        BigDecimal tasa = consultarTasas(par.origen()).get(par.destino().codigo());
        if (tasa == null) {
            throw new MonedaNoSoportadaException(par.destino());
        }
        if (tasa.signum() <= 0) {
            log.error("ExchangeRate-API devolvió una tasa inválida para {} -> {}: {}",
                    par.origen().codigo(), par.destino().codigo(), tasa);
            throw new TipoCambioNoDisponibleException();
        }
        return new TipoCambio(par, tasa);
    }

    private Map<String, BigDecimal> consultarTasas(Moneda base) {
        ExchangeRateApiResponse respuesta = invocarApi(base);
        if (respuesta == null || !RESULTADO_EXITOSO.equals(respuesta.result()) || respuesta.conversionRates() == null) {
            throw traducirError(base, HttpStatus.OK.value(), respuesta);
        }
        return respuesta.conversionRates();
    }

    private ExchangeRateApiResponse invocarApi(Moneda base) {
        try {
            return exchangeRateApiClient.getLatestRates(properties.key(), base.codigo());
        } catch (FeignException ex) {
            throw traducirError(base, ex.status(), leerCuerpoDeError(ex));
        }
    }

    private ExchangeRateApiResponse leerCuerpoDeError(FeignException ex) {
        try {
            return jsonMapper.readValue(ex.contentUTF8(), ExchangeRateApiResponse.class);
        } catch (JacksonException errorDeLectura) {
            return null;
        }
    }

    private DomainException traducirError(Moneda base, int status, ExchangeRateApiResponse respuesta) {
        String tipoError = respuesta == null ? null : respuesta.errorType();
        if (ERROR_MONEDA_NO_SOPORTADA.equals(tipoError)) {
            return new MonedaNoSoportadaException(base);
        }
        log.error("ExchangeRate-API no entregó tasas para {} (HTTP {}, error-type: {})", base.codigo(), status, tipoError);
        return new TipoCambioNoDisponibleException();
    }
}

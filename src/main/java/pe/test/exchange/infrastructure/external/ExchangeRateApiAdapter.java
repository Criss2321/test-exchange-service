package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiAdapter implements ProveedorTipoCambio {

    private static final String RESULTADO_EXITOSO = "success";
    private static final String ERROR_MONEDA_NO_SOPORTADA = "unsupported-code";

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final ExchangeRateApiProperties properties;
    private final JsonMapper jsonMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

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
        Supplier<ExchangeRateApiResponse> consulta =
                () -> exchangeRateApiClient.getLatestRates(properties.key(), base.codigo());
        try {
            return Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, consulta)).get();
        } catch (CallNotPermittedException ex) {
            log.warn("Circuito abierto: se omite la consulta a ExchangeRate-API para {}", base.codigo());
            throw new TipoCambioNoDisponibleException();
        } catch (FeignException ex) {
            if (ex.getCause() instanceof IOException causa) {
                log.error("ExchangeRate-API no respondió para {}: {}", base.codigo(), causa.toString());
                throw new TipoCambioNoDisponibleException();
            }
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

package pe.test.exchange.infrastructure.config;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienciaConfig {

    private static final String EXCHANGE_RATE_API = "exchangeRateApi";

    @Bean
    public CircuitBreaker circuitBreakerExchangeRateApi(ResilienciaProperties properties) {
        ResilienciaProperties.Circuito circuito = properties.circuito();
        return CircuitBreaker.of(EXCHANGE_RATE_API, CircuitBreakerConfig.custom()
                .slidingWindowSize(circuito.ventana())
                .minimumNumberOfCalls(circuito.llamadasMinimas())
                .failureRateThreshold(circuito.porcentajeFallos())
                .waitDurationInOpenState(circuito.esperaAbierto())
                .recordException(ResilienciaConfig::esFallaTransitoria)
                .build());
    }

    @Bean
    public Retry retryExchangeRateApi(ResilienciaProperties properties) {
        ResilienciaProperties.Reintento reintento = properties.reintento();
        return Retry.of(EXCHANGE_RATE_API, RetryConfig.custom()
                .maxAttempts(reintento.maxIntentos())
                .waitDuration(reintento.espera())
                .retryOnException(ResilienciaConfig::esFallaTransitoria)
                .build());
    }

    private static boolean esFallaTransitoria(Throwable ex) {
        return ex instanceof RetryableException || ex instanceof FeignException.FeignServerException;
    }
}

package pe.test.exchange.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "exchange-rate.api")
public record ExchangeRateApiProperties(

        @NotBlank(message = "Define la variable de entorno EXCHANGE_RATE_API_KEY con la API key de ExchangeRate-API")
        String key
) {
}

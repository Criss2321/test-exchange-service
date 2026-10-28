package pe.test.exchange.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI exchangeServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Exchange Service API")
                .version("1.0.0")
                .description("Conversión de montos entre monedas usando el tipo de cambio de ExchangeRate-API"));
    }
}

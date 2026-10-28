package pe.test.exchange.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.test.exchange.domain.service.ConversorMoneda;
import pe.test.exchange.domain.service.ProveedorTipoCambio;

import java.time.Clock;

@Configuration
public class DomainConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public ConversorMoneda conversorMoneda(ProveedorTipoCambio proveedorTipoCambio, Clock reloj) {
        return new ConversorMoneda(proveedorTipoCambio, reloj);
    }
}

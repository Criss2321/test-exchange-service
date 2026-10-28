package pe.test.exchange.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "exchange-rate.api.resiliencia")
public record ResilienciaProperties(@NotNull @Valid Reintento reintento, @NotNull @Valid Circuito circuito) {

    public record Reintento(@Positive int maxIntentos, @NotNull Duration espera) {
    }

    public record Circuito(@Positive int ventana, @Positive int llamadasMinimas, @Positive float porcentajeFallos,
                           @NotNull Duration esperaAbierto) {
    }
}

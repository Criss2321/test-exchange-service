package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.MonedaInvalidaException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Moneda(String codigo) {

    private static final Pattern FORMATO_ISO_4217 = Pattern.compile("^[A-Z]{3}$");

    public Moneda {
        if (codigo == null) {
            throw new MonedaInvalidaException(null);
        }
        codigo = codigo.trim().toUpperCase(Locale.ROOT);
        if (!FORMATO_ISO_4217.matcher(codigo).matches()) {
            throw new MonedaInvalidaException(codigo);
        }
    }
}

package pe.test.exchange.application.mapper;

import org.springframework.stereotype.Component;
import pe.test.exchange.application.dto.ConversionHistorialResponse;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.ParMonedas;

@Component
public class ConversionMapper {

    public Monto aMonto(ConversionRequest request) {
        return new Monto(request.monto());
    }

    public ParMonedas aParMonedas(ConversionRequest request) {
        return new ParMonedas(new Moneda(request.monedaOrigen()), new Moneda(request.monedaDestino()));
    }

    public ConversionResponse aResponse(Conversion conversion) {
        return new ConversionResponse(
                conversion.getMonto().valor(),
                conversion.getMontoConvertido(),
                conversion.getMonedaOrigen().codigo(),
                conversion.getMonedaDestino().codigo(),
                conversion.getTipoCambio().valor()
        );
    }

    public ConversionHistorialResponse aHistorialResponse(Conversion conversion) {
        return new ConversionHistorialResponse(
                conversion.getId(),
                conversion.getMonto().valor(),
                conversion.getMontoConvertido(),
                conversion.getMonedaOrigen().codigo(),
                conversion.getMonedaDestino().codigo(),
                conversion.getTipoCambio().valor(),
                conversion.getFechaOperacion()
        );
    }
}

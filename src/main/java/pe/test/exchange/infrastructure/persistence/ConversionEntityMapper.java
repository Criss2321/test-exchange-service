package pe.test.exchange.infrastructure.persistence;

import org.springframework.stereotype.Component;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.ParMonedas;
import pe.test.exchange.domain.model.TipoCambio;

@Component
public class ConversionEntityMapper {

    public ConversionEntity aEntidad(Conversion conversion) {
        ConversionEntity entidad = new ConversionEntity();
        entidad.setId(conversion.getId());
        entidad.setMonto(conversion.getMonto().valor());
        entidad.setMonedaOrigen(conversion.getMonedaOrigen().codigo());
        entidad.setMonedaDestino(conversion.getMonedaDestino().codigo());
        entidad.setTipoCambio(conversion.getTipoCambio().valor());
        entidad.setMontoConvertido(conversion.getMontoConvertido());
        entidad.setFechaOperacion(conversion.getFechaOperacion());
        return entidad;
    }

    public Conversion aDominio(ConversionEntity entidad) {
        ParMonedas par = new ParMonedas(new Moneda(entidad.getMonedaOrigen()), new Moneda(entidad.getMonedaDestino()));
        return Conversion.reconstituir(
                entidad.getId(),
                new Monto(entidad.getMonto()),
                new TipoCambio(par, entidad.getTipoCambio()),
                entidad.getMontoConvertido(),
                entidad.getFechaOperacion()
        );
    }
}

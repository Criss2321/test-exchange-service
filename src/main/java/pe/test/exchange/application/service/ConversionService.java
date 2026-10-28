package pe.test.exchange.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.test.exchange.application.dto.ConversionHistorialResponse;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.mapper.ConversionMapper;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ConversorMoneda;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ConversorMoneda conversorMoneda;
    private final ConversionRepository conversionRepository;
    private final ConversionMapper conversionMapper;

    public ConversionResponse convertir(ConversionRequest request) {
        Conversion conversion = conversorMoneda.convertir(
                conversionMapper.aMonto(request),
                conversionMapper.aParMonedas(request)
        );
        conversionRepository.guardar(conversion);
        return conversionMapper.aResponse(conversion);
    }

    public List<ConversionHistorialResponse> listarHistorial() {
        return conversionRepository.listarHistorial().stream()
                .map(conversionMapper::aHistorialResponse)
                .toList();
    }
}

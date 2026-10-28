package pe.test.exchange.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.repository.ConversionRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConversionRepositoryAdapter implements ConversionRepository {

    private final ConversionJpaRepository conversionJpaRepository;
    private final ConversionEntityMapper conversionEntityMapper;

    @Override
    public void guardar(Conversion conversion) {
        conversionJpaRepository.save(conversionEntityMapper.aEntidad(conversion));
    }

    @Override
    public List<Conversion> listarHistorial() {
        return conversionJpaRepository.findAllByOrderByFechaOperacionDesc().stream()
                .map(conversionEntityMapper::aDominio)
                .toList();
    }
}

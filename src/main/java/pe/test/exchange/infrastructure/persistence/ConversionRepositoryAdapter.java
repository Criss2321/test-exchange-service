package pe.test.exchange.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import pe.test.exchange.domain.exception.RegistroNoDisponibleException;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.repository.ConversionRepository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ConversionRepositoryAdapter implements ConversionRepository {

    private final ConversionJpaRepository conversionJpaRepository;
    private final ConversionEntityMapper conversionEntityMapper;

    @Override
    public void guardar(Conversion conversion) {
        try {
            conversionJpaRepository.save(conversionEntityMapper.aEntidad(conversion));
        } catch (TransientDataAccessException | DataAccessResourceFailureException
                 | CannotCreateTransactionException | TransactionSystemException ex) {
            log.error("No se pudo guardar la conversión {}", conversion.getId(), ex);
            throw new RegistroNoDisponibleException();
        }
    }

    @Override
    public List<Conversion> listarHistorial() {
        try {
            return conversionJpaRepository.findAllByOrderByFechaOperacionDesc().stream()
                    .map(conversionEntityMapper::aDominio)
                    .toList();
        } catch (TransientDataAccessException | DataAccessResourceFailureException
                 | CannotCreateTransactionException | TransactionSystemException ex) {
            log.error("No se pudo leer el historial de conversiones", ex);
            throw new RegistroNoDisponibleException();
        }
    }
}

package pe.test.exchange.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversiones")
@Getter
@Setter
@NoArgsConstructor
public class ConversionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, precision = 17, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda_origen", nullable = false, length = 3)
    private String monedaOrigen;

    @Column(name = "moneda_destino", nullable = false, length = 3)
    private String monedaDestino;

    @Column(name = "tipo_cambio", nullable = false, precision = 30, scale = 12)
    private BigDecimal tipoCambio;

    @Column(name = "monto_convertido", nullable = false, precision = 30, scale = 2)
    private BigDecimal montoConvertido;

    @Column(name = "fecha_operacion", nullable = false)
    private Instant fechaOperacion;
}

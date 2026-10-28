package pe.test.exchange.application.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.MonedaNoSoportadaException;
import pe.test.exchange.domain.exception.MonedasIgualesException;
import pe.test.exchange.domain.exception.MontoInvalidoException;
import pe.test.exchange.domain.exception.TipoCambioNoDisponibleException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TITULO_SOLICITUD_INVALIDA = "Solicitud inválida";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = crearProblema(status, TITULO_SOLICITUD_INVALIDA, "Uno o más campos no son válidos");
        problema.setProperty("errores", erroresPorCampo(ex));
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = crearProblema(status, TITULO_SOLICITUD_INVALIDA,
                "El cuerpo de la solicitud no es un JSON válido o tiene tipos de datos incorrectos");
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @ExceptionHandler({MontoInvalidoException.class, MonedaInvalidaException.class, MonedasIgualesException.class})
    public ProblemDetail manejarDatosInvalidos(DomainException ex) {
        return crearProblema(HttpStatus.BAD_REQUEST, TITULO_SOLICITUD_INVALIDA, ex.getMessage());
    }

    @ExceptionHandler(MonedaNoSoportadaException.class)
    public ProblemDetail manejarMonedaNoSoportada(MonedaNoSoportadaException ex) {
        return crearProblema(HttpStatus.UNPROCESSABLE_CONTENT, "Moneda no soportada", ex.getMessage());
    }

    @ExceptionHandler(TipoCambioNoDisponibleException.class)
    public ProblemDetail manejarTipoCambioNoDisponible(TipoCambioNoDisponibleException ex) {
        return crearProblema(HttpStatus.SERVICE_UNAVAILABLE, "Tipo de cambio no disponible", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado al procesar la solicitud", ex);
        return crearProblema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado, intenta nuevamente más tarde");
    }

    private ProblemDetail crearProblema(HttpStatusCode status, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        return problema;
    }

    private Map<String, String> erroresPorCampo(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "Valor inválido"));
        }
        return errores;
    }
}

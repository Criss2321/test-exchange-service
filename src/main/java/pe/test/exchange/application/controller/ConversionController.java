package pe.test.exchange.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.test.exchange.application.dto.ConversionHistorialResponse;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConversionService;

import java.util.List;

@RestController
@RequestMapping("/api/conversion")
@RequiredArgsConstructor
@Tag(name = "Conversión", description = "Conversión de montos entre monedas")
public class ConversionController {

    private final ConversionService conversionService;

    @PostMapping
    @Operation(summary = "Convierte un monto usando el tipo de cambio vigente y registra la operación")
    @ApiResponse(responseCode = "200", description = "Conversión realizada")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class),
            examples = {
                    @ExampleObject(name = "Campos inválidos", value = EjemplosError.CAMPOS_INVALIDOS),
                    @ExampleObject(name = "Monedas iguales", value = EjemplosError.MONEDAS_IGUALES)
            }))
    @ApiResponse(responseCode = "422", description = "Moneda no soportada por el proveedor", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = EjemplosError.MONEDA_NO_SOPORTADA)))
    @ApiResponse(responseCode = "503", description = "Proveedor de tipo de cambio o registro no disponible", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class),
            examples = {
                    @ExampleObject(name = "Proveedor no disponible", value = EjemplosError.PROVEEDOR_NO_DISPONIBLE),
                    @ExampleObject(name = "Registro no disponible", value = EjemplosError.REGISTRO_NO_DISPONIBLE)
            }))
    public ConversionResponse convertir(@Valid @RequestBody ConversionRequest request) {
        return conversionService.convertir(request);
    }

    @GetMapping("/historial")
    @Operation(summary = "Lista las conversiones registradas, de la más reciente a la más antigua")
    @ApiResponse(responseCode = "200", description = "Historial de conversiones")
    @ApiResponse(responseCode = "503", description = "Registro no disponible", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = EjemplosError.REGISTRO_NO_DISPONIBLE)))
    public List<ConversionHistorialResponse> listarHistorial() {
        return conversionService.listarHistorial();
    }
}

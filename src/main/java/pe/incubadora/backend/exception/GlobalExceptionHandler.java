package pe.incubadora.backend.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.incubadora.backend.dto.common.ApiErrorResponse;

/**
 * Traduce excepciones de la aplicacion a un formato JSON uniforme para la API REST.
 *
 * <p>Su objetivo didactico dentro del proyecto es mostrar como centralizar errores
 * tecnicos, de validacion y de negocio sin repetir logica en cada controller.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Convierte excepciones de dominio ya tipificadas en la respuesta estandar de la API.
     *
     * @param ex excepcion controlada lanzada por la aplicacion
     * @return respuesta con codigo funcional y estado HTTP asociado
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(ex.getCode(), ex.getMessage()));
    }

    /**
     * Resume el primer error de validacion sobre un cuerpo de request.
     *
     * @param ex detalle de los errores detectados por Bean Validation
     * @return error uniforme para el cliente
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Solicitud invalida";
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Solicitud invalida");
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("UNAUTHORIZED", "Credenciales invalidas"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("FORBIDDEN", "No tiene permisos para realizar esta operacion"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("RESOURCE_IN_USE", "No se pudo completar la operacion por integridad de datos"));
    }

    /**
     * Maneja fallos no previstos para evitar que se filtren detalles internos de la implementacion.
     *
     * @param ex excepcion no controlada
     * @return respuesta generica de error interno
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", "Ocurrio un error interno inesperado"));
    }
}

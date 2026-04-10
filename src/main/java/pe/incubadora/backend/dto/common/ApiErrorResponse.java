package pe.incubadora.backend.dto.common;

/**
 * Formato uniforme de error expuesto por la API REST.
 *
 * @param code codigo funcional estable para clasificar el error
 * @param message mensaje humano que explica el problema detectado
 */
public record ApiErrorResponse(
        String code,
        String message
) {
}

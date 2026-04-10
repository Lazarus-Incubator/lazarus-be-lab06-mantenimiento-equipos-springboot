package pe.incubadora.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Senala que el usuario autenticado no tiene permiso para ejecutar la operacion solicitada.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }
}

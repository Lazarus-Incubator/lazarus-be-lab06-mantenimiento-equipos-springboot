package pe.incubadora.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Representa errores de negocio detectados durante la ejecucion de un caso de uso.
 *
 * <p>Se usa cuando la solicitud es sintacticamente valida, pero incumple una regla
 * del dominio o una precondicion del flujo.</p>
 */
public class BusinessException extends ApiException {

    public BusinessException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        super(code, message, status);
    }
}

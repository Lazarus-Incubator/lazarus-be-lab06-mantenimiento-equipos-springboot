package pe.incubadora.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Indica que el recurso solicitado no existe o no pudo localizarse en la base de datos.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}

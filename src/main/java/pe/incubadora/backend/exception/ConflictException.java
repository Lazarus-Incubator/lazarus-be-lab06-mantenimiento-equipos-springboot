package pe.incubadora.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Indica un conflicto con el estado actual de los datos persistidos.
 *
 * <p>Suele emplearse para duplicados, unicidad o situaciones donde la operacion no
 * puede completarse sin resolver primero una colision de datos.</p>
 */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}

package pe.incubadora.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base para errores controlados que forman parte del contrato HTTP de la API.
 *
 * <p>Encapsula un codigo funcional y el estado HTTP que luego seran serializados por
 * el manejador global de errores.</p>
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    /**
     * Crea una excepcion de API con el detalle necesario para construir la respuesta JSON.
     *
     * @param code codigo estable que identifica la categoria del error
     * @param message mensaje legible para el cliente o consumidor de la API
     * @param status estado HTTP que debe devolverse
     */
    public ApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

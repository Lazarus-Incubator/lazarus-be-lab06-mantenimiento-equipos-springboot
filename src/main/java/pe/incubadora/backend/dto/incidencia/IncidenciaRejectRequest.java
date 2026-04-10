package pe.incubadora.backend.dto.incidencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request utilizado cuando una incidencia se rechaza en revision.
 *
 * @param comentario motivo funcional del rechazo, obligatorio para dejar trazabilidad
 */
public record IncidenciaRejectRequest(
        @NotBlank @Size(max = 500) String comentario
) {
}

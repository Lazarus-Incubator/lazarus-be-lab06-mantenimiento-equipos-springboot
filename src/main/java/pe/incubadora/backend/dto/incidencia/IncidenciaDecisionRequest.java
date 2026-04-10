package pe.incubadora.backend.dto.incidencia;

import jakarta.validation.constraints.Size;

/**
 * Request reutilizable para transiciones de revision que aceptan un comentario opcional.
 *
 * @param comentario observacion funcional registrada durante la decision
 */
public record IncidenciaDecisionRequest(
        @Size(max = 500) String comentario
) {
}

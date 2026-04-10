package pe.incubadora.backend.dto.incidencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.incubadora.backend.entity.PrioridadIncidencia;

/**
 * Request para editar una incidencia antes de que avance en el flujo.
 *
 * @param prioridad prioridad con la que se recalcula la fecha limite de atencion
 */
public record IncidenciaUpdateRequest(
        @NotBlank @Size(max = 160) String titulo,
        @NotBlank @Size(max = 1000) String descripcion,
        @NotNull PrioridadIncidencia prioridad
) {
}

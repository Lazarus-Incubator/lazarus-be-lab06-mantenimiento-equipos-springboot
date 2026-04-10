package pe.incubadora.backend.dto.incidencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.incubadora.backend.entity.PrioridadIncidencia;
import pe.incubadora.backend.entity.TipoIncidencia;

/**
 * Request para reportar una nueva incidencia sobre un equipo.
 *
 * <p>Representa la informacion minima que una sede u operaciones necesita enviar para
 * iniciar el flujo de atencion.</p>
 *
 * @param tipo categoria funcional del problema reportado
 * @param prioridad urgencia declarada, utilizada por el servicio para calcular la atencion
 * @param equipoId equipo afectado por la incidencia
 */
public record IncidenciaRequest(
        @NotBlank @Size(max = 160) String titulo,
        @NotBlank @Size(max = 1000) String descripcion,
        @NotNull TipoIncidencia tipo,
        @NotNull PrioridadIncidencia prioridad,
        @NotNull Long equipoId
) {
}

package pe.incubadora.backend.dto.incidencia;

import java.time.LocalDateTime;
import pe.incubadora.backend.dto.common.EquipoSummaryResponse;
import pe.incubadora.backend.dto.common.SedeSummaryResponse;
import pe.incubadora.backend.entity.EstadoIncidencia;
import pe.incubadora.backend.entity.PrioridadIncidencia;
import pe.incubadora.backend.entity.TipoIncidencia;

/**
 * Respuesta detallada de una incidencia dentro del flujo operativo.
 *
 * <p>Se usa tanto en listados como en consultas puntuales para mostrar el estado de
 * atencion, las fechas clave del SLA y el contexto del equipo afectado.</p>
 *
 * @param fechaLimiteAtencion fecha calculada por el sistema para priorizar la atencion
 * @param comentarioRevision observacion dejada durante la revision o decision
 * @param sede sede responsable de la incidencia
 * @param equipo equipo afectado por el incidente reportado
 */
public record IncidenciaResponse(
        Long id,
        String codigo,
        String titulo,
        String descripcion,
        TipoIncidencia tipo,
        PrioridadIncidencia prioridad,
        EstadoIncidencia estado,
        LocalDateTime fechaReporte,
        LocalDateTime fechaLimiteAtencion,
        LocalDateTime fechaRevision,
        LocalDateTime fechaResolucion,
        LocalDateTime fechaCierre,
        String comentarioRevision,
        SedeSummaryResponse sede,
        EquipoSummaryResponse equipo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

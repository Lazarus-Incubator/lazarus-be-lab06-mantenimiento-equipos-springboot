package pe.incubadora.backend.dto.orden;

import java.time.LocalDateTime;
import java.util.List;
import pe.incubadora.backend.dto.common.IncidenciaSummaryResponse;
import pe.incubadora.backend.dto.common.UserSummaryResponse;
import pe.incubadora.backend.entity.EstadoOrdenTrabajo;

/**
 * Respuesta detallada de una orden de trabajo.
 *
 * <p>Se utiliza para seguir la ejecucion tecnica de la orden, identificar a la
 * incidencia de origen, al tecnico responsable y el consumo de repuestos asociado.</p>
 *
 * @param estado etapa actual de la orden
 * @param incidencia incidencia que dio origen a la orden
 * @param tecnicoAsignado tecnico responsable cuando la orden ya fue asignada
 * @param repuestos detalle de repuestos registrados para la orden
 */
public record OrdenTrabajoResponse(
        Long id,
        String codigo,
        EstadoOrdenTrabajo estado,
        String descripcionTrabajo,
        String diagnosticoInicial,
        String observacionCierre,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        IncidenciaSummaryResponse incidencia,
        UserSummaryResponse tecnicoAsignado,
        List<OrdenTrabajoRepuestoResponse> repuestos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

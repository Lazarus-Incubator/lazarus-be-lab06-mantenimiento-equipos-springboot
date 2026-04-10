package pe.incubadora.backend.dto.common;

import pe.incubadora.backend.entity.EstadoIncidencia;
import pe.incubadora.backend.entity.PrioridadIncidencia;

/**
 * Resumen de incidencia utilizado dentro de respuestas mas grandes, como las ordenes de trabajo.
 *
 * @param codigo identificador visible de la incidencia
 * @param estado etapa actual del flujo de atencion
 * @param prioridad nivel de urgencia registrado para la incidencia
 */
public record IncidenciaSummaryResponse(
        Long id,
        String codigo,
        String titulo,
        EstadoIncidencia estado,
        PrioridadIncidencia prioridad,
        SedeSummaryResponse sede
) {
}

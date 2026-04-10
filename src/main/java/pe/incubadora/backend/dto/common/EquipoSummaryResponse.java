package pe.incubadora.backend.dto.common;

import pe.incubadora.backend.entity.EstadoEquipo;

/**
 * Vista resumida de un equipo usada como dato embebido en otras respuestas.
 *
 * @param codigoPatrimonial identificador operativo del equipo dentro de la organizacion
 * @param estado condicion operativa actual del equipo
 * @param sede sede a la que pertenece el equipo
 */
public record EquipoSummaryResponse(
        Long id,
        String codigoPatrimonial,
        String nombre,
        EstadoEquipo estado,
        boolean activo,
        SedeSummaryResponse sede
) {
}

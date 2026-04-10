package pe.incubadora.backend.dto.equipo;

import java.time.LocalDateTime;
import pe.incubadora.backend.dto.common.SedeSummaryResponse;
import pe.incubadora.backend.entity.EstadoEquipo;

/**
 * Respuesta completa de un equipo de laboratorio expuesta por la API.
 *
 * @param estado estado operativo actual del equipo
 * @param sede sede propietaria del equipo
 * @param createdAt fecha de creacion del registro
 * @param updatedAt ultima fecha de actualizacion del registro
 */
public record EquipoLaboratorioResponse(
        Long id,
        String codigoPatrimonial,
        String nombre,
        String marca,
        String modelo,
        String numeroSerie,
        String area,
        boolean activo,
        EstadoEquipo estado,
        SedeSummaryResponse sede,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

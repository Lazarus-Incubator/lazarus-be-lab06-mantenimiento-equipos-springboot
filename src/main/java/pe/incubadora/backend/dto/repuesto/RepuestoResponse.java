package pe.incubadora.backend.dto.repuesto;

import java.time.LocalDateTime;

/**
 * Respuesta completa de un repuesto dentro del modulo de inventario.
 *
 * @param stockBajo indicador calculado para identificar si el repuesto requiere seguimiento
 * @param version version de locking optimista asociada al registro persistido
 */
public record RepuestoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String unidadMedida,
        Integer stockActual,
        Integer stockMinimo,
        boolean stockBajo,
        boolean activo,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

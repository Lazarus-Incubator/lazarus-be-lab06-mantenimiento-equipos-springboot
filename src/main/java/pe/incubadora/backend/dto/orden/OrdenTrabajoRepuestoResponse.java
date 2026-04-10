package pe.incubadora.backend.dto.orden;

import java.time.LocalDateTime;
import pe.incubadora.backend.dto.repuesto.RepuestoResponse;

/**
 * Respuesta que describe el consumo de un repuesto dentro de una orden de trabajo.
 *
 * @param cantidad unidades registradas para ese repuesto
 * @param repuesto detalle del repuesto consumido
 */
public record OrdenTrabajoRepuestoResponse(
        Long id,
        Integer cantidad,
        RepuestoResponse repuesto,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

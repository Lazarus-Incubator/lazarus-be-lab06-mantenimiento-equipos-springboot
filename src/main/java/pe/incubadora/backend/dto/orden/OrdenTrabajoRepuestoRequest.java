package pe.incubadora.backend.dto.orden;

import jakarta.validation.constraints.NotNull;

/**
 * Item de consumo enviado al finalizar una orden.
 *
 * @param repuestoId repuesto que se desea descontar del inventario
 * @param cantidad unidades consumidas durante la atencion
 */
public record OrdenTrabajoRepuestoRequest(
        @NotNull Long repuestoId,
        @NotNull Integer cantidad
) {
}

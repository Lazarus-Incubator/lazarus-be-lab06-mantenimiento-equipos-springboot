package pe.incubadora.backend.dto.orden;

import jakarta.validation.constraints.NotNull;

/**
 * Request utilizado para asignar una orden a un tecnico.
 *
 * @param tecnicoId usuario tecnico que asumira la ejecucion de la orden
 */
public record OrdenTrabajoAssignRequest(
        @NotNull Long tecnicoId
) {
}

package pe.incubadora.backend.dto.orden;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request para cerrar tecnicamente una orden de trabajo.
 *
 * <p>Combina la observacion final del tecnico con el detalle de repuestos consumidos,
 * por lo que participa en uno de los flujos mas sensibles del sistema.</p>
 *
 * @param observacionCierre conclusion registrada al finalizar la atencion
 * @param repuestos repuestos consumidos durante la ejecucion de la orden
 */
public record OrdenTrabajoFinalizeRequest(
        @NotBlank @Size(max = 1000) String observacionCierre,
        @Valid List<OrdenTrabajoRepuestoRequest> repuestos
) {
}

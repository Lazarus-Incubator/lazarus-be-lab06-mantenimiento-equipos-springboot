package pe.incubadora.backend.dto.orden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request para generar una orden de trabajo desde una incidencia existente.
 *
 * @param incidenciaId incidencia origen que justifica la intervencion tecnica
 * @param descripcionTrabajo trabajo planificado o requerido para atender el caso
 * @param diagnosticoInicial observacion inicial del area operativa antes de asignar la orden
 */
public record OrdenTrabajoRequest(
        @NotNull Long incidenciaId,
        @NotBlank @Size(max = 1000) String descripcionTrabajo,
        @Size(max = 1000) String diagnosticoInicial
) {
}

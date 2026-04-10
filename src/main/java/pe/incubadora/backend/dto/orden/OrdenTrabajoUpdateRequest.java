package pe.incubadora.backend.dto.orden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para ajustar la descripcion o el diagnostico inicial de una orden en preparacion.
 */
public record OrdenTrabajoUpdateRequest(
        @NotBlank @Size(max = 1000) String descripcionTrabajo,
        @Size(max = 1000) String diagnosticoInicial
) {
}

package pe.incubadora.backend.dto.repuesto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request para registrar o actualizar repuestos del inventario tecnico.
 *
 * @param unidadMedida forma en la que se contabiliza el repuesto dentro del stock
 * @param stockActual cantidad disponible al momento de guardar el registro
 * @param stockMinimo umbral de referencia usado para alertar stock bajo
 */
public record RepuestoRequest(
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 500) String descripcion,
        @NotBlank @Size(max = 30) String unidadMedida,
        @NotNull @Min(0) Integer stockActual,
        @NotNull @Min(0) Integer stockMinimo,
        @NotNull Boolean activo
) {
}

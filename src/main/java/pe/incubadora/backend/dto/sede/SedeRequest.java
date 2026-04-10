package pe.incubadora.backend.dto.sede;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request usado para altas y ediciones de sedes.
 *
 * @param codigo identificador corto de la sede dentro de la empresa
 * @param activa bandera logica que determina si la sede puede operar en el sistema
 */
public record SedeRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 80) String ciudad,
        @NotBlank @Size(max = 250) String direccion,
        @NotNull Boolean activa
) {
}

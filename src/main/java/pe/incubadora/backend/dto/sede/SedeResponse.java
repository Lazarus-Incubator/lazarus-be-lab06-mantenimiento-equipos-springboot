package pe.incubadora.backend.dto.sede;

import java.time.LocalDateTime;

/**
 * Respuesta completa de una sede del laboratorio.
 *
 * @param activa indica si la sede se encuentra disponible para operar en el flujo
 * @param createdAt fecha de creacion del registro
 * @param updatedAt ultima fecha de actualizacion del registro
 */
public record SedeResponse(
        Long id,
        String codigo,
        String nombre,
        String ciudad,
        String direccion,
        boolean activa,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

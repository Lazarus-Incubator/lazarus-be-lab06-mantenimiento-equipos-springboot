package pe.incubadora.backend.dto.common;

import pe.incubadora.backend.entity.Role;

/**
 * Resumen de usuario empleado en respuestas donde interesa identificar al actor asignado.
 *
 * @param role rol funcional del usuario dentro del sistema
 */
public record UserSummaryResponse(
        Long id,
        String email,
        String nombreCompleto,
        Role role
) {
}

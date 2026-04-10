package pe.incubadora.backend.dto.auth;

import pe.incubadora.backend.entity.Role;

/**
 * Resumen del usuario autenticado que acompana la respuesta de login.
 *
 * @param role rol con el que se evaluaran permisos y visibilidad dentro de la API
 * @param sedeId sede asociada al usuario cuando aplica para el flujo funcional
 */
public record AuthenticatedUserResponse(
        Long id,
        String email,
        String nombreCompleto,
        Role role,
        Long sedeId,
        String sedeNombre
) {
}

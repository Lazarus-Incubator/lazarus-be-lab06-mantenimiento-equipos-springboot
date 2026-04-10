package pe.incubadora.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pe.incubadora.backend.entity.Role;
import pe.incubadora.backend.exception.ForbiddenException;

/**
 * Utilidad de acceso rapido al contexto de seguridad actual.
 *
 * <p>Se usa desde services para consultar el usuario autenticado y resolver reglas
 * de visibilidad sin acoplar la logica de negocio a clases HTTP.</p>
 */
@Component
public class SecurityUtils {

    /**
     * Obtiene el principal autenticado esperado por la aplicacion.
     *
     * @return usuario autenticado actual
     * @throws ForbiddenException si no existe autenticacion valida en el contexto
     */
    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("FORBIDDEN", "No se pudo obtener el usuario autenticado");
        }
        return principal;
    }

    /**
     * Evalua si el usuario actual posee alguno de los roles indicados.
     *
     * @param roles roles aceptados por el caso de uso
     * @return {@code true} cuando el principal actual coincide con alguno de ellos
     */
    public boolean hasAnyRole(Role... roles) {
        UserPrincipal principal = getCurrentUser();
        for (Role role : roles) {
            if (principal.getRole() == role) {
                return true;
            }
        }
        return false;
    }
}

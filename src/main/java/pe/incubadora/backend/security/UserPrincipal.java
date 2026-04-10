package pe.incubadora.backend.security;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.incubadora.backend.entity.Role;
import pe.incubadora.backend.entity.UserEntity;

/**
 * Implementacion de {@link UserDetails} que adapta el modelo de usuarios del dominio
 * al mecanismo de autenticacion de Spring Security.
 *
 * <p>Expone datos adicionales utiles para la aplicacion, como el rol y la sede
 * asociada, que luego se reutilizan en validaciones de autorizacion.</p>
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String nombreCompleto;
    private final Role role;
    private final Long sedeId;
    private final boolean active;

    /**
     * Construye el principal de seguridad a partir de la entidad persistida.
     *
     * @param user usuario recuperado desde la base de datos
     */
    public UserPrincipal(UserEntity user) {
        this.id = user.getId();
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.nombreCompleto = user.getNombreCompleto();
        this.role = user.getRole();
        this.sedeId = user.getSede() != null ? user.getSede().getId() : null;
        this.active = user.isActivo();
    }

    /**
     * Expone el rol del usuario como autoridad de Spring Security.
     *
     * @return coleccion con la autoridad derivada del rol del dominio
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

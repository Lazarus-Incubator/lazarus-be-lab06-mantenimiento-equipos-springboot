package pe.incubadora.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.repository.UserRepository;

/**
 * Adaptador entre el repositorio de usuarios y el contrato {@link UserDetailsService}.
 *
 * <p>Permite que Spring Security recupere usuarios activos desde la base de datos y
 * los convierta al tipo {@link UserPrincipal} usado por la aplicacion.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Busca un usuario activo por correo para autenticarlo dentro de Spring Security.
     *
     * @param username correo utilizado como identificador de login
     * @return principal de seguridad construido desde la entidad de usuario
     * @throws UsernameNotFoundException cuando no existe un usuario activo con ese correo
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCaseAndActivoTrue(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}

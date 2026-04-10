package pe.incubadora.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.config.AppJwtProperties;
import pe.incubadora.backend.dto.auth.LoginRequest;
import pe.incubadora.backend.dto.auth.LoginResponse;
import pe.incubadora.backend.entity.UserEntity;
import pe.incubadora.backend.exception.NotFoundException;
import pe.incubadora.backend.mapper.UserMapper;
import pe.incubadora.backend.repository.UserRepository;
import pe.incubadora.backend.security.JwtService;
import pe.incubadora.backend.security.UserPrincipal;

/**
 * Coordina el flujo de autenticacion de la API.
 *
 * <p>Esta clase valida las credenciales con Spring Security, recupera el usuario
 * persistido y construye la respuesta de login con el token JWT y el resumen del
 * usuario autenticado.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AppJwtProperties appJwtProperties;

    /**
     * Autentica a un usuario y prepara la respuesta utilizada por el cliente para
     * iniciar una sesion autenticada.
     *
     * @param request credenciales enviadas al endpoint de login
     * @return token JWT, tiempo de expiracion y datos basicos del usuario autenticado
     * @throws NotFoundException si el usuario autenticado ya no existe en la base de datos
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));

        return new LoginResponse(
                "Bearer",
                jwtService.generateToken(principal),
                appJwtProperties.expirationMinutes(),
                userMapper.toAuthenticatedResponse(user)
        );
    }
}

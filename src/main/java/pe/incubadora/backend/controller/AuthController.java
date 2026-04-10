package pe.incubadora.backend.controller;

import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dto.auth.LoginRequest;
import pe.incubadora.backend.service.AuthService;

/**
 * Expone las operaciones de autenticación de la API.
 *
 * <p>En este proyecto el flujo de entrada consiste en validar credenciales y,
 * si son correctas, devolver un token JWT junto con el resumen del usuario
 * autenticado.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Autentica a un usuario y devuelve el token de acceso para las siguientes
     * llamadas protegidas.
     *
     * @param request credenciales recibidas desde el formulario o cliente API
     * @return respuesta con el token emitido y los datos básicos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales invalidas"));
        }
    }
}

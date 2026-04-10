package pe.incubadora.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request utilizado por el endpoint de autenticacion.
 *
 * @param email correo con el que el usuario inicia sesion
 * @param password clave enviada para validar la identidad del usuario
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}

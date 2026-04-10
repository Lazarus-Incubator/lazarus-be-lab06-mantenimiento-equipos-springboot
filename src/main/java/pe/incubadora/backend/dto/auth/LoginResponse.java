package pe.incubadora.backend.dto.auth;

/**
 * Respuesta devuelta despues de un login exitoso.
 *
 * <p>Agrupa el token emitido por la API y un resumen del usuario autenticado para
 * que el cliente pueda iniciar su sesion de trabajo.</p>
 *
 * @param tokenType esquema del token, normalmente Bearer
 * @param accessToken token JWT que debe enviarse en peticiones posteriores
 * @param expiresInMinutes tiempo de vigencia expresado en minutos
 * @param user datos basicos del usuario autenticado
 */
public record LoginResponse(
        String tokenType,
        String accessToken,
        Long expiresInMinutes,
        AuthenticatedUserResponse user
) {
}

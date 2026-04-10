package pe.incubadora.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.config.AppJwtProperties;

/**
 * Encapsula la generacion y validacion de tokens JWT usados por la API.
 *
 * <p>Ademas de firmar el token, define los claims minimos que la aplicacion necesita
 * para reconstruir el contexto del usuario autenticado.</p>
 */
@Service
public class JwtService {

    private final AppJwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(AppJwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un JWT para el usuario autenticado.
     *
     * @param principal principal de seguridad autenticado
     * @return token firmado listo para enviarlo al cliente
     */
    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("userId", principal.getId())
                .claim("role", principal.getRole().name())
                .claim("sedeId", principal.getSedeId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Obtiene el identificador principal del token, usado como nombre de usuario.
     *
     * @param token token JWT emitido por la aplicacion
     * @return username contenido en el claim principal
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Comprueba que el token corresponda al usuario esperado y que no haya expirado.
     *
     * @param token token JWT recibido en la peticion
     * @param principal usuario contra el cual se valida el token
     * @return {@code true} cuando el token sigue vigente y pertenece al usuario indicado
     */
    public boolean isTokenValid(String token, UserPrincipal principal) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equalsIgnoreCase(principal.getUsername())
                && claims.getExpiration().after(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

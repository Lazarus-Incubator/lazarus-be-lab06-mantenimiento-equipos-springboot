package pe.incubadora.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que inspecciona el encabezado {@code Authorization} y autentica la peticion
 * cuando encuentra un JWT valido.
 *
 * <p>Se ejecuta una sola vez por request y, si el token es correcto, deja cargado el
 * usuario autenticado en el {@code SecurityContext} para el resto del flujo.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Extrae el token Bearer, valida su contenido y registra la autenticacion resultante.
     *
     * @param request request HTTP entrante
     * @param response response HTTP saliente
     * @param filterChain cadena de filtros de Spring Security
     * @throws ServletException si ocurre un error del contenedor servlet
     * @throws IOException si falla la lectura o escritura del flujo HTTP
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtService.extractUsername(token);
                UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, principal)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

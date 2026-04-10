package pe.incubadora.backend.dto.common;

/**
 * Representacion minima de una sede para respuestas que necesitan contexto organizacional.
 */
public record SedeSummaryResponse(
        Long id,
        String codigo,
        String nombre,
        String ciudad
) {
}

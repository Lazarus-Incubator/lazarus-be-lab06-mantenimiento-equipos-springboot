package pe.incubadora.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * Genera codigos legibles para entidades operativas del dominio.
 *
 * <p>Se utiliza para producir identificadores visibles como incidencias y ordenes
 * de trabajo, combinando un prefijo funcional, una marca temporal y un sufijo
 * aleatorio corto.</p>
 */
@Service
public class CodeGeneratorService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Construye un codigo con el prefijo indicado y una marca temporal.
     *
     * @param prefix prefijo funcional que identifica el tipo de entidad, por ejemplo {@code INC} u {@code OT}
     * @return codigo generado para mostrarlo en el flujo operativo
     */
    public String generate(String prefix) {
        int suffix = ThreadLocalRandom.current().nextInt(100, 999);
        return prefix + "-" + LocalDateTime.now().format(FORMATTER) + "-" + suffix;
    }
}

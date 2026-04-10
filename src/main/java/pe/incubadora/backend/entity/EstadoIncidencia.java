package pe.incubadora.backend.entity;

/**
 * Etapas del ciclo de vida de una incidencia desde su registro hasta su cierre.
 */
public enum EstadoIncidencia {
    REGISTRADA,
    EN_REVISION,
    APROBADA,
    RECHAZADA,
    EN_ATENCION,
    RESUELTA,
    CERRADA
}

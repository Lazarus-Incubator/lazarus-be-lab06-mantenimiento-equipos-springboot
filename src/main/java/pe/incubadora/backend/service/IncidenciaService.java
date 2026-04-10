package pe.incubadora.backend.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dto.incidencia.IncidenciaDecisionRequest;
import pe.incubadora.backend.dto.incidencia.IncidenciaRejectRequest;
import pe.incubadora.backend.dto.incidencia.IncidenciaRequest;
import pe.incubadora.backend.dto.incidencia.IncidenciaResponse;
import pe.incubadora.backend.dto.incidencia.IncidenciaUpdateRequest;
import pe.incubadora.backend.entity.EquipoLaboratorio;
import pe.incubadora.backend.entity.EstadoEquipo;
import pe.incubadora.backend.entity.EstadoIncidencia;
import pe.incubadora.backend.entity.Incidencia;
import pe.incubadora.backend.entity.PrioridadIncidencia;
import pe.incubadora.backend.entity.Role;
import pe.incubadora.backend.exception.BusinessException;
import pe.incubadora.backend.exception.ConflictException;
import pe.incubadora.backend.exception.ForbiddenException;
import pe.incubadora.backend.exception.NotFoundException;
import pe.incubadora.backend.mapper.IncidenciaMapper;
import pe.incubadora.backend.repository.IncidenciaRepository;
import pe.incubadora.backend.repository.OrdenTrabajoRepository;
import pe.incubadora.backend.security.SecurityUtils;

/**
 * Orquesta el ciclo de vida de las incidencias reportadas sobre equipos de laboratorio.
 *
 * <p>En esta clase conviven filtros de consulta, reglas de visibilidad por rol,
 * validaciones previas al registro y transiciones de estado del flujo de atencion.</p>
 */
@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final EquipoLaboratorioService equipoLaboratorioService;
    private final IncidenciaMapper incidenciaMapper;
    private final SecurityUtils securityUtils;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * Lista incidencias aplicando filtros operativos y restricciones derivadas del rol.
     *
     * @param sedeId sede solicitada por el cliente; para el rol {@code SEDE} se fuerza la sede propia
     * @param equipoId equipo asociado a la incidencia
     * @param estado estado actual dentro del flujo de atencion
     * @param prioridad prioridad registrada para la incidencia
     * @param tipo categoria funcional del incidente reportado
     * @param pageable configuracion de paginacion y ordenamiento
     * @return pagina de incidencias visibles para el usuario autenticado
     */
    @Transactional(readOnly = true)
    public Page<IncidenciaResponse> findAll(Long sedeId,
                                            Long equipoId,
                                            EstadoIncidencia estado,
                                            PrioridadIncidencia prioridad,
                                            pe.incubadora.backend.entity.TipoIncidencia tipo,
                                            Pageable pageable) {
        if (securityUtils.hasAnyRole(Role.TECNICO)) {
            throw new ForbiddenException("FORBIDDEN", "El técnico no puede consultar incidencias directamente");
        }

        Specification<Incidencia> spec = (root, query, cb) -> cb.conjunction();
        Long visibleSedeId = resolveVisibleSedeId(sedeId);

        if (visibleSedeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sede").get("id"), visibleSedeId));
        }
        if (equipoId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("equipo").get("id"), equipoId));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (prioridad != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("prioridad"), prioridad));
        }
        if (tipo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }

        return incidenciaRepository.findAll(spec, pageable).map(incidenciaMapper::toResponse);
    }

    /**
     * Obtiene una incidencia puntual validando su visibilidad segun el rol.
     *
     * @param id identificador de la incidencia
     * @return incidencia expuesta como DTO de respuesta
     */
    @Transactional(readOnly = true)
    public IncidenciaResponse findById(Long id) {
        if (securityUtils.hasAnyRole(Role.SEDE)) {
            return incidenciaMapper.toResponse(getEntity(id));
        }
        return incidenciaMapper.toResponse(getVisibleEntity(id));
    }

    /**
     * Registra una nueva incidencia para un equipo de laboratorio.
     *
     * <p>Durante este flujo se verifica la disponibilidad del equipo, se inicializa
     * el estado del proceso y se calcula automaticamente la fecha limite de atencion
     * a partir de la prioridad recibida.</p>
     *
     * @param request datos enviados por la API para reportar la incidencia
     * @return incidencia creada con su codigo operativo y fechas calculadas
     */
    @Transactional
    public IncidenciaResponse create(IncidenciaRequest request) {
        EquipoLaboratorio equipo = equipoLaboratorioService.getVisibleEntity(request.equipoId());

        if (!equipo.getSede().isActiva()) {
            throw new BusinessException("SEDE_INACTIVE", "No se puede crear una incidencia en una sede inactiva");
        }
        if (!equipo.isActivo()) {
            throw new BusinessException("EQUIPO_NO_ACTIVO", "No se puede crear una incidencia para un equipo inactivo");
        }
        if (incidenciaRepository.existsByEquipoIdAndTipoAndEstadoIn(
                equipo.getId(),
                request.tipo(),
                java.util.List.of(EstadoIncidencia.REGISTRADA, EstadoIncidencia.EN_REVISION)
        )) {
            throw new ConflictException("INCIDENCIA_DUPLICADA", "Ya existe una incidencia abierta para el mismo equipo y tipo");
        }

        LocalDateTime ahora = LocalDateTime.now();
        Incidencia incidencia = new Incidencia();
        incidencia.setCodigo(codeGeneratorService.generate("INC"));
        incidencia.setTitulo(request.titulo().trim());
        incidencia.setDescripcion(request.descripcion().trim());
        incidencia.setTipo(request.tipo());
        incidencia.setPrioridad(request.prioridad());
        incidencia.setEstado(EstadoIncidencia.REGISTRADA);
        incidencia.setFechaReporte(ahora);
        incidencia.setFechaLimiteAtencion(calculateDeadline(ahora, request.prioridad()));
        incidencia.setEquipo(equipo);
        incidencia.setSede(equipo.getSede());

        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }

    /**
     * Modifica una incidencia mientras siga en su etapa inicial.
     *
     * @param id identificador de la incidencia
     * @param request nuevos datos editables de la incidencia
     * @return incidencia actualizada
     * @throws BusinessException cuando el flujo ya no permite modificaciones
     */
    @Transactional
    public IncidenciaResponse update(Long id, IncidenciaUpdateRequest request) {
        Incidencia incidencia = getVisibleEntity(id);
        if (incidencia.getEstado() != EstadoIncidencia.REGISTRADA) {
            throw new BusinessException("INCIDENCIA_UPDATE_INVALID", "Solo se puede editar una incidencia en estado REGISTRADA");
        }

        incidenciaMapper.updateEntity(request, incidencia);
        incidencia.setFechaLimiteAtencion(calculateDeadline(incidencia.getFechaReporte(), request.prioridad()));
        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }

    /**
     * Elimina una incidencia solo en estados previos al tratamiento tecnico y cuando
     * aun no existe una orden de trabajo asociada.
     *
     * @param id identificador de la incidencia
     */
    @Transactional
    public void delete(Long id) {
        Incidencia incidencia = getVisibleEntity(id);
        if (!(incidencia.getEstado() == EstadoIncidencia.REGISTRADA || incidencia.getEstado() == EstadoIncidencia.RECHAZADA)) {
            throw new BusinessException("INCIDENCIA_DELETE_INVALID", "Solo se puede eliminar una incidencia REGISTRADA o RECHAZADA");
        }
        if (ordenTrabajoRepository.existsByIncidenciaId(id)) {
            throw new ConflictException("INCIDENCIA_WITH_ORDER", "No se puede eliminar una incidencia asociada a una orden de trabajo");
        }
        incidenciaRepository.delete(incidencia);
    }

    /**
     * Mueve una incidencia desde el registro inicial hacia la etapa de revision.
     *
     * @param id identificador de la incidencia
     * @param request comentario de revision opcional
     * @return incidencia luego de la transicion
     */
    @Transactional
    public IncidenciaResponse moveToRevision(Long id, IncidenciaDecisionRequest request) {
        Incidencia incidencia = getEntity(id);
        if (incidencia.getEstado() != EstadoIncidencia.REGISTRADA) {
            throw new BusinessException("INCIDENCIA_TRANSITION_INVALID", "Solo se puede pasar a revisión desde REGISTRADA");
        }
        incidencia.setEstado(EstadoIncidencia.EN_REVISION);
        incidencia.setFechaRevision(LocalDateTime.now());
        incidencia.setComentarioRevision(request.comentario());
        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }

    /**
     * Aprueba una incidencia revisada para habilitar la generacion de una orden de trabajo.
     *
     * @param id identificador de la incidencia
     * @param request comentario dejado durante la decision
     * @return incidencia aprobada
     */
    @Transactional
    public IncidenciaResponse approve(Long id, IncidenciaDecisionRequest request) {
        Incidencia incidencia = getEntity(id);
        if (incidencia.getEstado() != EstadoIncidencia.EN_REVISION) {
            throw new BusinessException("INCIDENCIA_TRANSITION_INVALID", "Solo se puede aprobar una incidencia desde EN_REVISION");
        }
        incidencia.setEstado(EstadoIncidencia.APROBADA);
        incidencia.setComentarioRevision(request.comentario());
        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }

    /**
     * Rechaza una incidencia revisada y registra el comentario asociado a la decision.
     *
     * @param id identificador de la incidencia
     * @param request comentario obligatorio de rechazo
     * @return incidencia rechazada
     */
    @Transactional
    public IncidenciaResponse reject(Long id, IncidenciaRejectRequest request) {
        Incidencia incidencia = getEntity(id);
        if (incidencia.getEstado() != EstadoIncidencia.EN_REVISION) {
            throw new BusinessException("INCIDENCIA_TRANSITION_INVALID", "Solo se puede rechazar una incidencia desde EN_REVISION");
        }
        incidencia.setEstado(EstadoIncidencia.RECHAZADA);
        incidencia.setComentarioRevision(request.comentario());
        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }

    /**
     * Cierra una incidencia una vez que ya fue resuelta en el flujo operativo.
     *
     * @param id identificador de la incidencia
     * @return incidencia cerrada con su fecha de cierre
     */
    @Transactional
    public IncidenciaResponse close(Long id) {
        Incidencia incidencia = getVisibleEntity(id);
        if (incidencia.getEstado() != EstadoIncidencia.RESUELTA) {
            throw new BusinessException("INCIDENCIA_TRANSITION_INVALID", "Solo se puede cerrar una incidencia desde RESUELTA");
        }
        incidencia.setEstado(EstadoIncidencia.CERRADA);
        incidencia.setFechaCierre(LocalDateTime.now());
        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }

    /**
     * Recupera la entidad JPA y valida que el usuario tenga permisos para verla.
     *
     * @param id identificador de la incidencia
     * @return entidad visible para el usuario actual
     */
    @Transactional(readOnly = true)
    public Incidencia getVisibleEntity(Long id) {
        Incidencia incidencia = getEntity(id);
        validateVisibility(incidencia);
        return incidencia;
    }

    /**
     * Obtiene la entidad de incidencia sin aplicar filtros de visibilidad.
     *
     * <p>Se utiliza desde otros servicios cuando la regla de acceso se resuelve en una
     * capa superior o cuando el flujo requiere operar sobre la entidad persistida.</p>
     *
     * @param id identificador de la incidencia
     * @return entidad encontrada en base de datos
     * @throws NotFoundException si no existe una incidencia con el identificador indicado
     */
    @Transactional(readOnly = true)
    public Incidencia getEntity(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("INCIDENCIA_NOT_FOUND", "Incidencia no encontrada"));
    }

    private Long resolveVisibleSedeId(Long requestedSedeId) {
        if (securityUtils.hasAnyRole(Role.SEDE)) {
            Long currentSedeId = securityUtils.getCurrentUser().getSedeId();
            if (requestedSedeId != null && !requestedSedeId.equals(currentSedeId)) {
                throw new ForbiddenException("FORBIDDEN", "No puede consultar incidencias de otra sede");
            }
            return currentSedeId;
        }
        return requestedSedeId;
    }

    private void validateVisibility(Incidencia incidencia) {
        if (securityUtils.hasAnyRole(Role.SEDE)
                && !incidencia.getSede().getId().equals(securityUtils.getCurrentUser().getSedeId())) {
            throw new ForbiddenException("FORBIDDEN", "No puede acceder a incidencias de otra sede");
        }
        if (securityUtils.hasAnyRole(Role.TECNICO)) {
            throw new ForbiddenException("FORBIDDEN", "El técnico no puede acceder a incidencias directamente");
        }
    }

    private LocalDateTime calculateDeadline(LocalDateTime fechaReporte, PrioridadIncidencia prioridad) {
        return switch (prioridad) {
            case CRITICA -> fechaReporte.plusHours(8);
            case ALTA -> fechaReporte.plusHours(4);
            case MEDIA -> fechaReporte.plusHours(24);
            case BAJA -> fechaReporte.plusHours(48);
        };
    }
}

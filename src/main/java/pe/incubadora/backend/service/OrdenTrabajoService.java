package pe.incubadora.backend.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dto.orden.OrdenTrabajoAssignRequest;
import pe.incubadora.backend.dto.orden.OrdenTrabajoFinalizeRequest;
import pe.incubadora.backend.dto.orden.OrdenTrabajoRequest;
import pe.incubadora.backend.dto.orden.OrdenTrabajoResponse;
import pe.incubadora.backend.dto.orden.OrdenTrabajoRepuestoRequest;
import pe.incubadora.backend.dto.orden.OrdenTrabajoUpdateRequest;
import pe.incubadora.backend.entity.EstadoIncidencia;
import pe.incubadora.backend.entity.EstadoOrdenTrabajo;
import pe.incubadora.backend.entity.Incidencia;
import pe.incubadora.backend.entity.OrdenTrabajo;
import pe.incubadora.backend.entity.OrdenTrabajoRepuesto;
import pe.incubadora.backend.entity.Repuesto;
import pe.incubadora.backend.entity.Role;
import pe.incubadora.backend.entity.UserEntity;
import pe.incubadora.backend.exception.BusinessException;
import pe.incubadora.backend.exception.ConflictException;
import pe.incubadora.backend.exception.ForbiddenException;
import pe.incubadora.backend.exception.NotFoundException;
import pe.incubadora.backend.mapper.OrdenTrabajoMapper;
import pe.incubadora.backend.repository.OrdenTrabajoRepository;
import pe.incubadora.backend.repository.OrdenTrabajoRepuestoRepository;
import pe.incubadora.backend.repository.RepuestoRepository;
import pe.incubadora.backend.repository.UserRepository;
import pe.incubadora.backend.security.SecurityUtils;
import pe.incubadora.backend.security.UserPrincipal;

/**
 * Administra el flujo tecnico de las ordenes de trabajo derivadas de incidencias.
 *
 * <p>Este servicio concentra la creacion, asignacion, ejecucion y cierre de ordenes,
 * junto con las verificaciones de acceso por rol y el registro del consumo de repuestos.</p>
 */
@Service
@RequiredArgsConstructor
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final OrdenTrabajoRepuestoRepository ordenTrabajoRepuestoRepository;
    private final RepuestoRepository repuestoRepository;
    private final UserRepository userRepository;
    private final IncidenciaService incidenciaService;
    private final RepuestoService repuestoService;
    private final OrdenTrabajoMapper ordenTrabajoMapper;
    private final SecurityUtils securityUtils;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * Lista ordenes de trabajo visibles para el usuario autenticado.
     *
     * <p>Los filtros se combinan con restricciones por sede o tecnico segun el rol
     * presente en el contexto de seguridad.</p>
     *
     * @param sedeId sede solicitada para la consulta
     * @param incidenciaId incidencia origen de la orden
     * @param estado estado operativo de la orden
     * @param tecnicoId tecnico asignado
     * @param pageable configuracion de paginacion
     * @return pagina de ordenes con sus repuestos ya mapeados a DTO
     */
    @Transactional(readOnly = true)
    public Page<OrdenTrabajoResponse> findAll(Long sedeId,
                                              Long incidenciaId,
                                              EstadoOrdenTrabajo estado,
                                              Long tecnicoId,
                                              Pageable pageable) {
        Specification<OrdenTrabajo> spec = (root, query, cb) -> cb.conjunction();

        if (securityUtils.hasAnyRole(Role.SEDE)) {
            Long currentSedeId = securityUtils.getCurrentUser().getSedeId();
            if (sedeId != null && !sedeId.equals(currentSedeId)) {
                throw new ForbiddenException("FORBIDDEN", "No puede consultar órdenes de otra sede");
            }
            sedeId = currentSedeId;
        }
        if (securityUtils.hasAnyRole(Role.TECNICO)) {
            Long currentTechnicianId = securityUtils.getCurrentUser().getId();
            if (tecnicoId != null && !tecnicoId.equals(currentTechnicianId)) {
                throw new ForbiddenException("FORBIDDEN", "No puede consultar órdenes de otro técnico");
            }
            tecnicoId = currentTechnicianId;
        }
        if (sedeId != null) {
            Long finalSedeId = sedeId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("incidencia").get("sede").get("id"), finalSedeId));
        }
        if (incidenciaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("incidencia").get("id"), incidenciaId));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (tecnicoId != null) {
            Long finalTecnicoId = tecnicoId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tecnicoAsignado").get("id"), finalTecnicoId));
        }

        return ordenTrabajoRepository.findAll(spec, pageable)
                .map(orden -> ordenTrabajoMapper.toResponse(orden, ordenTrabajoRepuestoRepository.findByOrdenTrabajoId(orden.getId())));
    }

    /**
     * Obtiene una orden puntual respetando las reglas de visibilidad del rol actual.
     *
     * @param id identificador de la orden
     * @return orden con sus repuestos asociados
     */
    @Transactional(readOnly = true)
    public OrdenTrabajoResponse findById(Long id) {
        OrdenTrabajo ordenTrabajo = securityUtils.hasAnyRole(Role.SEDE) ? getEntity(id) : getVisibleEntity(id);
        return ordenTrabajoMapper.toResponse(ordenTrabajo, ordenTrabajoRepuestoRepository.findByOrdenTrabajoId(id));
    }

    /**
     * Genera una orden de trabajo a partir de una incidencia existente.
     *
     * <p>Ademas de crear la orden, este flujo actualiza el estado de la incidencia para
     * reflejar que ya ingreso a la etapa de atencion tecnica.</p>
     *
     * @param request datos iniciales de la orden
     * @return orden recien creada
     */
    @Transactional
    public OrdenTrabajoResponse create(OrdenTrabajoRequest request) {
        Incidencia incidencia = incidenciaService.getEntity(request.incidenciaId());
        if (incidencia.getEstado() == EstadoIncidencia.RECHAZADA || incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            throw new BusinessException("ORDER_CREATE_INVALID", "La incidencia no se encuentra disponible para generar una orden");
        }
        if (ordenTrabajoRepository.existsByIncidenciaId(incidencia.getId())) {
            throw new ConflictException("ORDER_ALREADY_EXISTS", "La incidencia ya tiene una orden de trabajo asociada");
        }

        OrdenTrabajo ordenTrabajo = new OrdenTrabajo();
        ordenTrabajo.setCodigo(codeGeneratorService.generate("OT"));
        ordenTrabajo.setIncidencia(incidencia);
        ordenTrabajo.setEstado(EstadoOrdenTrabajo.CREADA);
        ordenTrabajo.setDescripcionTrabajo(request.descripcionTrabajo().trim());
        ordenTrabajo.setDiagnosticoInicial(request.diagnosticoInicial());
        ordenTrabajoRepository.save(ordenTrabajo);

        incidencia.setEstado(EstadoIncidencia.EN_ATENCION);

        return ordenTrabajoMapper.toResponse(ordenTrabajo, List.of());
    }

    /**
     * Actualiza una orden mientras permanezca en su etapa de preparacion.
     *
     * @param id identificador de la orden
     * @param request datos editables de la orden
     * @return orden actualizada
     */
    @Transactional
    public OrdenTrabajoResponse update(Long id, OrdenTrabajoUpdateRequest request) {
        OrdenTrabajo ordenTrabajo = getVisibleEntity(id);
        if (ordenTrabajo.getEstado() != EstadoOrdenTrabajo.CREADA) {
            throw new BusinessException("ORDER_UPDATE_INVALID", "Solo se puede editar una orden en estado CREADA");
        }
        ordenTrabajoMapper.updateEntity(request, ordenTrabajo);
        return ordenTrabajoMapper.toResponse(ordenTrabajoRepository.save(ordenTrabajo), List.of());
    }

    /**
     * Elimina una orden en estado inicial y revierte la incidencia a la etapa previa.
     *
     * @param id identificador de la orden
     */
    @Transactional
    public void delete(Long id) {
        OrdenTrabajo ordenTrabajo = getVisibleEntity(id);
        if (ordenTrabajo.getEstado() != EstadoOrdenTrabajo.CREADA) {
            throw new BusinessException("ORDER_DELETE_INVALID", "Solo se puede eliminar una orden en estado CREADA");
        }
        ordenTrabajo.getIncidencia().setEstado(EstadoIncidencia.APROBADA);
        ordenTrabajoRepuestoRepository.deleteByOrdenTrabajoId(id);
        ordenTrabajoRepository.delete(ordenTrabajo);
    }

    /**
     * Asigna la orden a un tecnico activo.
     *
     * @param id identificador de la orden
     * @param request tecnico que se desea asignar
     * @return orden con la asignacion aplicada
     */
    @Transactional
    public OrdenTrabajoResponse assign(Long id, OrdenTrabajoAssignRequest request) {
        OrdenTrabajo ordenTrabajo = getVisibleEntity(id);
        if (ordenTrabajo.getEstado() == EstadoOrdenTrabajo.FINALIZADA) {
            throw new BusinessException("ORDER_TRANSITION_INVALID", "No se puede asignar una orden finalizada");
        }

        UserEntity tecnico = userRepository.findById(request.tecnicoId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Técnico no encontrado"));
        if (tecnico.getRole() != Role.TECNICO || !tecnico.isActivo()) {
            throw new BusinessException("TECHNICIAN_INVALID", "El usuario seleccionado no es un técnico activo");
        }

        ordenTrabajo.setTecnicoAsignado(tecnico);
        ordenTrabajo.setEstado(EstadoOrdenTrabajo.ASIGNADA);
        ordenTrabajo.setFechaAsignacion(LocalDateTime.now());
        return ordenTrabajoMapper.toResponse(ordenTrabajoRepository.save(ordenTrabajo), List.of());
    }

    /**
     * Marca el inicio de la ejecucion tecnica de una orden asignada.
     *
     * @param id identificador de la orden
     * @return orden en curso
     */
    @Transactional
    public OrdenTrabajoResponse start(Long id) {
        OrdenTrabajo ordenTrabajo = getVisibleEntity(id);
        if (ordenTrabajo.getEstado() == EstadoOrdenTrabajo.FINALIZADA) {
            throw new BusinessException("ORDER_TRANSITION_INVALID", "No se puede iniciar una orden finalizada");
        }
        validateTechnicianAction(ordenTrabajo);
        ordenTrabajo.setEstado(EstadoOrdenTrabajo.EN_PROCESO);
        ordenTrabajo.setFechaInicio(LocalDateTime.now());
        return ordenTrabajoMapper.toResponse(ordenTrabajoRepository.save(ordenTrabajo), List.of());
    }

    /**
     * Finaliza una orden registrando observaciones de cierre y consumo de repuestos.
     *
     * <p>Es uno de los flujos mas sensibles del sistema porque actualiza simultaneamente
     * la orden, la incidencia asociada y el stock de los repuestos utilizados.</p>
     *
     * @param id identificador de la orden
     * @param request observacion final y lista de repuestos consumidos
     * @return orden finalizada con el detalle de repuestos asociados
     */
    public OrdenTrabajoResponse finalizeOrder(Long id, OrdenTrabajoFinalizeRequest request) {
        OrdenTrabajo ordenTrabajo = getVisibleEntity(id);
        if (ordenTrabajo.getEstado() == EstadoOrdenTrabajo.CREADA || ordenTrabajo.getEstado() == EstadoOrdenTrabajo.ASIGNADA) {
            throw new BusinessException("ORDER_TRANSITION_INVALID", "La orden todavia no esta lista para finalizarse");
        }
        validateTechnicianAction(ordenTrabajo);
        validateDuplicatedRepuestos(request.repuestos());

        List<OrdenTrabajoRepuestoRequest> requestedRepuestos = request.repuestos() != null ? request.repuestos() : List.of();
        for (OrdenTrabajoRepuestoRequest requestedRepuesto : requestedRepuestos) {
            Repuesto repuesto = repuestoService.getEntity(requestedRepuesto.repuestoId());
            if (!repuesto.isActivo()) {
                throw new BusinessException("REPUESTO_INACTIVE", "No se puede consumir un repuesto inactivo");
            }
            if (repuesto.getStockActual() < requestedRepuesto.cantidad()) {
                throw new BusinessException("STOCK_INSUFFICIENT", "Stock insuficiente para completar la orden");
            }
            repuesto.setStockActual(repuesto.getStockActual() - requestedRepuesto.cantidad());
            repuestoRepository.saveAndFlush(repuesto);

            OrdenTrabajoRepuesto ordenTrabajoRepuesto = new OrdenTrabajoRepuesto();
            ordenTrabajoRepuesto.setOrdenTrabajo(ordenTrabajo);
            ordenTrabajoRepuesto.setRepuesto(repuesto);
            ordenTrabajoRepuesto.setCantidad(requestedRepuesto.cantidad());
            ordenTrabajoRepuestoRepository.save(ordenTrabajoRepuesto);
        }

        ordenTrabajo.setEstado(EstadoOrdenTrabajo.FINALIZADA);
        ordenTrabajo.setObservacionCierre(request.observacionCierre().trim());
        ordenTrabajo.setFechaFin(LocalDateTime.now());
        ordenTrabajo.getIncidencia().setEstado(EstadoIncidencia.RESUELTA);
        ordenTrabajo.getIncidencia().setFechaResolucion(LocalDateTime.now());

        OrdenTrabajo saved = ordenTrabajoRepository.save(ordenTrabajo);
        return ordenTrabajoMapper.toResponse(saved, ordenTrabajoRepuestoRepository.findByOrdenTrabajoId(saved.getId()));
    }

    /**
     * Recupera una orden sin aplicar reglas de visibilidad adicionales.
     *
     * @param id identificador de la orden
     * @return entidad persistida
     * @throws NotFoundException si la orden no existe
     */
    @Transactional(readOnly = true)
    public OrdenTrabajo getEntity(Long id) {
        return ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Orden de trabajo no encontrada"));
    }

    /**
     * Recupera una orden y comprueba que el usuario autenticado pueda trabajar con ella.
     *
     * @param id identificador de la orden
     * @return entidad visible para el rol actual
     */
    @Transactional(readOnly = true)
    public OrdenTrabajo getVisibleEntity(Long id) {
        OrdenTrabajo ordenTrabajo = getEntity(id);
        validateVisibility(ordenTrabajo);
        return ordenTrabajo;
    }

    private void validateVisibility(OrdenTrabajo ordenTrabajo) {
        UserPrincipal currentUser = securityUtils.getCurrentUser();
        if (currentUser.getRole() == Role.SEDE
                && !ordenTrabajo.getIncidencia().getSede().getId().equals(currentUser.getSedeId())) {
            throw new ForbiddenException("FORBIDDEN", "No puede acceder a órdenes de otra sede");
        }
        if (currentUser.getRole() == Role.TECNICO
                && (ordenTrabajo.getTecnicoAsignado() == null || !ordenTrabajo.getTecnicoAsignado().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("FORBIDDEN", "Solo puede acceder a órdenes asignadas a usted");
        }
    }

    private void validateTechnicianAction(OrdenTrabajo ordenTrabajo) {
        UserPrincipal currentUser = securityUtils.getCurrentUser();
        if (currentUser.getRole() == Role.TECNICO
                && (ordenTrabajo.getTecnicoAsignado() == null || !ordenTrabajo.getTecnicoAsignado().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("FORBIDDEN", "Solo el técnico asignado puede ejecutar esta transición");
        }
    }

    private void validateDuplicatedRepuestos(List<OrdenTrabajoRepuestoRequest> repuestos) {
        if (repuestos == null || repuestos.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (OrdenTrabajoRepuestoRequest repuesto : repuestos) {
            if (!ids.add(repuesto.repuestoId())) {
                throw new BusinessException("DUPLICATED_REPLACEMENT", "No se puede registrar el mismo repuesto más de una vez en la orden");
            }
        }
    }
}

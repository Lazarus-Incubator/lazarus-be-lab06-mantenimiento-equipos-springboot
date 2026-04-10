package pe.incubadora.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pe.incubadora.backend.dto.equipo.EquipoLaboratorioRequest;
import pe.incubadora.backend.dto.equipo.EquipoLaboratorioResponse;
import pe.incubadora.backend.entity.EquipoLaboratorio;
import pe.incubadora.backend.entity.EstadoEquipo;
import pe.incubadora.backend.entity.Role;
import pe.incubadora.backend.entity.Sede;
import pe.incubadora.backend.exception.ConflictException;
import pe.incubadora.backend.exception.ForbiddenException;
import pe.incubadora.backend.exception.NotFoundException;
import pe.incubadora.backend.mapper.EquipoLaboratorioMapper;
import pe.incubadora.backend.repository.EquipoLaboratorioRepository;
import pe.incubadora.backend.security.SecurityUtils;

/**
 * Gestiona el catalogo de equipos de laboratorio y las restricciones de visibilidad
 * asociadas a la sede del usuario.
 *
 * <p>Ademas del CRUD, este servicio centraliza la validacion de acceso cuando un
 * usuario de tipo {@code SEDE} intenta operar sobre equipos fuera de su alcance.</p>
 */
@Service
@RequiredArgsConstructor
public class EquipoLaboratorioService {

    private final EquipoLaboratorioRepository equipoRepository;
    private final EquipoLaboratorioMapper equipoMapper;
    private final SedeService sedeService;
    private final SecurityUtils securityUtils;

    /**
     * Lista equipos aplicando filtros operativos y restricciones de acceso por sede.
     *
     * @param search texto libre usado sobre codigo patrimonial y datos descriptivos
     * @param sedeId sede solicitada por el cliente; puede ajustarse segun el rol autenticado
     * @param estado estado operativo del equipo
     * @param activo indicador de vigencia logica
     * @param pageable configuracion de paginacion y ordenamiento
     * @return pagina de equipos visibles para el usuario actual
     */
    @Transactional(readOnly = true)
    public Page<EquipoLaboratorioResponse> findAll(String search,
                                                   Long sedeId,
                                                   EstadoEquipo estado,
                                                   Boolean activo,
                                                   Pageable pageable) {
        Specification<EquipoLaboratorio> spec = (root, query, cb) -> cb.conjunction();
        Long visibleSedeId = resolveVisibleSedeId(sedeId);

        if (StringUtils.hasText(search)) {
            String value = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("codigoPatrimonial")), value),
                    cb.like(cb.lower(root.get("nombre")), value),
                    cb.like(cb.lower(root.get("marca")), value),
                    cb.like(cb.lower(root.get("modelo")), value)
            ));
        }
        if (visibleSedeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sede").get("id"), visibleSedeId));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (activo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("activo"), activo));
        }

        return equipoRepository.findAll(spec, pageable).map(equipoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EquipoLaboratorioResponse findById(Long id) {
        return equipoMapper.toResponse(getVisibleEntity(id));
    }

    /**
     * Registra un nuevo equipo y lo vincula a la sede indicada en el request.
     *
     * @param request datos enviados por la API para crear el equipo
     * @return equipo persistido listo para exponerlo como DTO de respuesta
     * @throws ConflictException si el codigo patrimonial ya esta en uso
     */
    @Transactional
    public EquipoLaboratorioResponse create(EquipoLaboratorioRequest request) {
        if (equipoRepository.existsByCodigoPatrimonialIgnoreCase(request.codigoPatrimonial().trim())) {
            throw new ConflictException("EQUIPO_CODE_EXISTS", "Ya existe un equipo con el código patrimonial indicado");
        }

        Sede sede = sedeService.getVisibleEntity(request.sedeId());
        EquipoLaboratorio equipo = new EquipoLaboratorio();
        equipoMapper.updateEntity(request, equipo);
        equipo.setSede(sede);
        return equipoMapper.toResponse(equipoRepository.save(equipo));
    }

    /**
     * Actualiza la informacion editable de un equipo respetando la visibilidad por sede.
     *
     * @param id identificador del equipo
     * @param request nuevos datos del equipo
     * @return representacion actualizada del equipo
     */
    @Transactional
    public EquipoLaboratorioResponse update(Long id, EquipoLaboratorioRequest request) {
        EquipoLaboratorio equipo = getVisibleEntity(id);
        if (!equipo.getCodigoPatrimonial().equalsIgnoreCase(request.codigoPatrimonial().trim())
                && equipoRepository.existsByCodigoPatrimonialIgnoreCase(request.codigoPatrimonial().trim())) {
            throw new ConflictException("EQUIPO_CODE_EXISTS", "Ya existe un equipo con el código patrimonial indicado");
        }

        Sede sede = sedeService.getVisibleEntity(request.sedeId());
        equipoMapper.updateEntity(request, equipo);
        equipo.setSede(sede);
        return equipoMapper.toResponse(equipoRepository.save(equipo));
    }

    @Transactional
    public void delete(Long id) {
        equipoRepository.delete(getVisibleEntity(id));
    }

    /**
     * Recupera la entidad y valida que el usuario autenticado tenga acceso a su sede.
     *
     * <p>Este metodo se usa internamente cuando otras capas necesitan trabajar con la
     * entidad JPA sin exponerla directamente por la API.</p>
     *
     * @param id identificador del equipo
     * @return entidad del equipo con acceso validado
     * @throws NotFoundException si el equipo no existe
     * @throws ForbiddenException si la sede del equipo no es visible para el usuario actual
     */
    @Transactional(readOnly = true)
    public EquipoLaboratorio getVisibleEntity(Long id) {
        EquipoLaboratorio equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("EQUIPO_NOT_FOUND", "Equipo de laboratorio no encontrado"));
        validateSedeVisibility(equipo.getSede().getId());
        return equipo;
    }

    private Long resolveVisibleSedeId(Long requestedSedeId) {
        if (securityUtils.hasAnyRole(Role.SEDE)) {
            Long currentSedeId = securityUtils.getCurrentUser().getSedeId();
            if (requestedSedeId != null && !requestedSedeId.equals(currentSedeId)) {
                throw new ForbiddenException("FORBIDDEN", "No puede consultar equipos de otra sede");
            }
            return currentSedeId;
        }
        return requestedSedeId;
    }

    private void validateSedeVisibility(Long sedeId) {
        if (securityUtils.hasAnyRole(Role.SEDE) && !sedeId.equals(securityUtils.getCurrentUser().getSedeId())) {
            throw new ForbiddenException("FORBIDDEN", "No puede acceder a equipos de otra sede");
        }
    }
}

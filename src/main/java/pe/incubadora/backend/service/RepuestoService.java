package pe.incubadora.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pe.incubadora.backend.dto.repuesto.RepuestoRequest;
import pe.incubadora.backend.dto.repuesto.RepuestoResponse;
import pe.incubadora.backend.entity.Repuesto;
import pe.incubadora.backend.exception.ConflictException;
import pe.incubadora.backend.exception.NotFoundException;
import pe.incubadora.backend.mapper.RepuestoMapper;
import pe.incubadora.backend.repository.RepuestoRepository;

/**
 * Administra el catalogo de repuestos y sus consultas operativas.
 *
 * <p>Su responsabilidad principal es mantener la informacion base de inventario y
 * proyectarla como DTOs listos para la API.</p>
 */
@Service
@RequiredArgsConstructor
public class RepuestoService {

    private final RepuestoRepository repuestoRepository;
    private final RepuestoMapper repuestoMapper;

    /**
     * Lista repuestos aplicando filtros por texto, estado logico y condicion de stock.
     *
     * @param search texto libre sobre codigo, nombre y descripcion
     * @param activo filtro por vigencia logica del repuesto
     * @param stockBajo cuando es verdadero restringe la consulta a repuestos cercanos al minimo
     * @param pageable configuracion de paginacion
     * @return pagina de repuestos mapeados a DTO
     */
    @Transactional(readOnly = true)
    public Page<RepuestoResponse> findAll(String search, Boolean activo, Boolean stockBajo, Pageable pageable) {
        Specification<Repuesto> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(search)) {
            String value = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("codigo")), value),
                    cb.like(cb.lower(root.get("nombre")), value),
                    cb.like(cb.lower(root.get("descripcion")), value)
            ));
        }
        if (activo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("activo"), activo));
        }
        if (Boolean.TRUE.equals(stockBajo)) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("stockActual"), root.get("stockMinimo")));
        }

        return repuestoRepository.findAll(spec, pageable).map(repuestoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RepuestoResponse findById(Long id) {
        return repuestoMapper.toResponse(getEntity(id));
    }

    /**
     * Registra un repuesto nuevo en el inventario.
     *
     * @param request datos del repuesto enviados por la API
     * @return repuesto creado
     */
    @Transactional
    public RepuestoResponse create(RepuestoRequest request) {
        if (repuestoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw new ConflictException("REPUESTO_CODE_EXISTS", "Ya existe un repuesto con el código indicado");
        }
        Repuesto repuesto = new Repuesto();
        repuestoMapper.updateEntity(request, repuesto);
        return repuestoMapper.toResponse(repuestoRepository.save(repuesto));
    }

    /**
     * Actualiza un repuesto existente manteniendo la unicidad del codigo.
     *
     * @param id identificador del repuesto
     * @param request nuevos datos del repuesto
     * @return repuesto actualizado
     */
    @Transactional
    public RepuestoResponse update(Long id, RepuestoRequest request) {
        Repuesto repuesto = getEntity(id);
        if (!repuesto.getCodigo().equalsIgnoreCase(request.codigo().trim())
                && repuestoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw new ConflictException("REPUESTO_CODE_EXISTS", "Ya existe un repuesto con el código indicado");
        }
        repuestoMapper.updateEntity(request, repuesto);
        return repuestoMapper.toResponse(repuestoRepository.save(repuesto));
    }

    @Transactional
    public void delete(Long id) {
        repuestoRepository.delete(getEntity(id));
    }

    /**
     * Obtiene la entidad JPA del repuesto para procesos internos que necesitan trabajar
     * con stock, version y demas atributos persistidos.
     *
     * @param id identificador del repuesto
     * @return entidad encontrada en base de datos
     */
    @Transactional(readOnly = true)
    public Repuesto getEntity(Long id) {
        return repuestoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("REPUESTO_NOT_FOUND", "Repuesto no encontrado"));
    }
}

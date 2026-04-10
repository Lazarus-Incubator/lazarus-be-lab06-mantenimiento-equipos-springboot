package pe.incubadora.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dto.repuesto.RepuestoRequest;
import pe.incubadora.backend.dto.repuesto.RepuestoResponse;
import pe.incubadora.backend.entity.Repuesto;
import pe.incubadora.backend.service.RepuestoService;

/**
 * Expone el catálogo de repuestos y consultas de inventario.
 *
 * <p>Su uso principal es sostener la gestión de stock y el consumo asociado a
 * las órdenes de trabajo.</p>
 */
@RestController
@RequestMapping("/api/v1/repuestos")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class RepuestoController {

    private final RepuestoService repuestoService;

    /**
     * Lista repuestos con filtros administrativos y paginación.
     *
     * @return página de repuestos disponibles en la API
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public Page<RepuestoResponse> findAll(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) Boolean activo,
                                          @RequestParam(required = false) Boolean stockBajo,
                                          @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return repuestoService.findAll(search, activo, stockBajo, pageable);
    }

    /**
     * Devuelve un repuesto puntual por identificador.
     *
     * @param id repuesto solicitado
     * @return entidad representada en la respuesta actual del endpoint
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public Repuesto findById(@PathVariable Long id) {
        return repuestoService.getEntity(id);
    }

    /**
     * Registra un repuesto nuevo en el catálogo de inventario.
     *
     * @param request datos necesarios para crear el recurso
     * @return repuesto creado
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public ResponseEntity<RepuestoResponse> create(@Valid @RequestBody RepuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repuestoService.create(request));
    }

    /**
     * Actualiza la ficha administrativa de un repuesto.
     *
     * @param id repuesto a modificar
     * @param request estado deseado del recurso
     * @return repuesto actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public RepuestoResponse update(@PathVariable Long id, @Valid @RequestBody RepuestoRequest request) {
        return repuestoService.update(id, request);
    }

    /**
     * Elimina un repuesto del catálogo expuesto.
     *
     * @param id identificador del repuesto
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repuestoService.delete(id);
    }

    /**
     * Obtiene los repuestos que la API considera en situación de stock bajo.
     *
     * @return página filtrada para seguimiento operativo del inventario
     */
    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public Page<RepuestoResponse> findLowStock(@RequestParam(required = false) String search,
                                               @RequestParam(required = false) Boolean activo,
                                               @PageableDefault(sort = "stockActual", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RepuestoResponse> page = repuestoService.findAll(search, activo, null, pageable);
        var filtered = page.getContent().stream()
                .filter(repuesto -> repuesto.stockActual() < repuesto.stockMinimo())
                .toList();
        return new PageImpl<>(filtered, pageable, filtered.size());
    }
}

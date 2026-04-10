package pe.incubadora.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import pe.incubadora.backend.dto.equipo.EquipoLaboratorioRequest;
import pe.incubadora.backend.dto.equipo.EquipoLaboratorioResponse;
import pe.incubadora.backend.entity.EstadoEquipo;
import pe.incubadora.backend.service.EquipoLaboratorioService;

/**
 * Publica las operaciones HTTP para administrar equipos de laboratorio.
 *
 * <p>Desde esta capa se exponen búsquedas paginadas y operaciones CRUD sobre
 * el inventario técnico de cada sede.</p>
 */
@RestController
@RequestMapping("/api/v1/equipos")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class EquipoLaboratorioController {

    private final EquipoLaboratorioService equipoService;

    /**
     * Lista equipos con filtros opcionales y paginación.
     *
     * @return página de equipos visibles para el usuario autenticado
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public Page<EquipoLaboratorioResponse> findAll(@RequestParam(required = false) String search,
                                                   @RequestParam(required = false) Long sedeId,
                                                   @RequestParam(required = false) EstadoEquipo estado,
                                                   @RequestParam(required = false) Boolean activo,
                                                   @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return equipoService.findAll(search, sedeId, estado, activo, pageable);
    }

    /**
     * Recupera el detalle de un equipo concreto.
     *
     * @param id identificador del equipo
     * @return representación del equipo solicitada por la API
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public EquipoLaboratorioResponse findById(@PathVariable Long id) {
        return equipoService.findById(id);
    }

    /**
     * Registra un nuevo equipo dentro del catálogo de la organización.
     *
     * @param request datos funcionales y de ubicación del equipo
     * @return recurso creado con su información consolidada
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public ResponseEntity<EquipoLaboratorioResponse> create(@Valid @RequestBody EquipoLaboratorioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipoService.create(request));
    }

    /**
     * Actualiza los datos operativos de un equipo existente.
     *
     * @param id identificador del equipo a modificar
     * @param request nuevo estado de la información editable
     * @return equipo actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public EquipoLaboratorioResponse update(@PathVariable Long id, @Valid @RequestBody EquipoLaboratorioRequest request) {
        return equipoService.update(id, request);
    }

    /**
     * Elimina un equipo del catálogo expuesto por la API.
     *
     * @param id identificador del equipo a eliminar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        equipoService.delete(id);
    }
}

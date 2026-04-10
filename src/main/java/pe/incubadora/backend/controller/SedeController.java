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
import pe.incubadora.backend.dto.sede.SedeRequest;
import pe.incubadora.backend.dto.sede.SedeResponse;
import pe.incubadora.backend.entity.Sede;
import pe.incubadora.backend.service.SedeService;

/**
 * Controlador REST para la administración de sedes de AndeLab.
 *
 * <p>La información de sede se utiliza como contexto organizacional para
 * equipos, incidencias y restricciones de visibilidad.</p>
 */
@RestController
@RequestMapping("/api/v1/sedes")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SedeController {

    private final SedeService sedeService;

    /**
     * Lista sedes con filtros básicos y paginación.
     *
     * @return página de sedes visibles para el usuario autenticado
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public Page<SedeResponse> findAll(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) Boolean activa,
                                      @PageableDefault(sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return sedeService.findAll(search, activa, pageable);
    }

    /**
     * Recupera una sede concreta por identificador.
     *
     * @param id sede solicitada
     * @return representación actual devuelta por el endpoint
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public Sede findById(@PathVariable Long id) {
        return sedeService.getVisibleEntity(id);
    }

    /**
     * Crea una nueva sede dentro de la red.
     *
     * @param request datos administrativos de la sede
     * @return sede creada
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SedeResponse> create(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sedeService.create(request));
    }

    /**
     * Actualiza la información de una sede existente.
     *
     * @param id identificador de la sede
     * @param request nueva información de la sede
     * @return sede actualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SedeResponse update(@PathVariable Long id, @Valid @RequestBody SedeRequest request) {
        return sedeService.update(id, request);
    }

    /**
     * Elimina una sede del catálogo administrativo.
     *
     * @param id identificador de la sede
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sedeService.delete(id);
    }
}

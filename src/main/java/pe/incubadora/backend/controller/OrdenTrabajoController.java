package pe.incubadora.backend.controller;

import java.util.Map;
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
import pe.incubadora.backend.dto.orden.OrdenTrabajoAssignRequest;
import pe.incubadora.backend.dto.orden.OrdenTrabajoFinalizeRequest;
import pe.incubadora.backend.dto.orden.OrdenTrabajoRequest;
import pe.incubadora.backend.dto.orden.OrdenTrabajoResponse;
import pe.incubadora.backend.dto.orden.OrdenTrabajoUpdateRequest;
import pe.incubadora.backend.entity.EstadoOrdenTrabajo;
import pe.incubadora.backend.service.OrdenTrabajoService;

/**
 * Expone el flujo HTTP de las órdenes de trabajo técnicas.
 *
 * <p>Las operaciones de esta clase cubren consulta, creación y transiciones
 * operativas utilizadas por operaciones y técnicos.</p>
 */
@RestController
@RequestMapping("/api/v1/ordenes")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrdenTrabajoController {

    private final OrdenTrabajoService ordenTrabajoService;

    /**
     * Lista órdenes con filtros de sede, incidencia, estado y técnico.
     *
     * @return página de órdenes visibles según el rol autenticado
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE','TECNICO')")
    public Page<OrdenTrabajoResponse> findAll(@RequestParam(required = false) Long sedeId,
                                              @RequestParam(required = false) Long incidenciaId,
                                              @RequestParam(required = false) EstadoOrdenTrabajo estado,
                                              @RequestParam(required = false) Long tecnicoId,
                                              @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ordenTrabajoService.findAll(sedeId, incidenciaId, estado, tecnicoId, pageable);
    }

    /**
     * Recupera una orden por identificador.
     *
     * @param id identificador interno de la orden
     * @return detalle de la orden o una respuesta de error según el flujo actual
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE','TECNICO')")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ordenTrabajoService.findById(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "detail", "Orden no encontrada"));
        }
    }

    /**
     * Crea una orden de trabajo a partir de una incidencia.
     *
     * @param request datos base con los que inicia la atención técnica
     * @return orden creada
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public ResponseEntity<OrdenTrabajoResponse> create(@Valid @RequestBody OrdenTrabajoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenTrabajoService.create(request));
    }

    /**
     * Actualiza la información editable de una orden.
     *
     * @param id orden a modificar
     * @param request contenido actualizado
     * @return orden con los cambios aplicados
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public OrdenTrabajoResponse update(@PathVariable Long id, @Valid @RequestBody OrdenTrabajoUpdateRequest request) {
        return ordenTrabajoService.update(id, request);
    }

    /**
     * Elimina una orden cuando el flujo operativo todavía lo admite.
     *
     * @param id identificador de la orden
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ordenTrabajoService.delete(id);
    }

    /**
     * Asigna la orden a un técnico responsable.
     *
     * @param id orden a asignar
     * @param request técnico que asumirá la atención
     * @return orden con la asignación realizada
     */
    @PostMapping("/{id}/asignar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public OrdenTrabajoResponse assign(@PathVariable Long id, @Valid @RequestBody OrdenTrabajoAssignRequest request) {
        return ordenTrabajoService.assign(id, request);
    }

    /**
     * Marca el inicio formal del trabajo técnico.
     *
     * @param id orden que entra en ejecución
     * @return orden en proceso
     */
    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN','TECNICO')")
    public OrdenTrabajoResponse start(@PathVariable Long id) {
        return ordenTrabajoService.start(id);
    }

    /**
     * Cierra técnicamente la orden y registra los repuestos consumidos.
     *
     * @param id orden a finalizar
     * @param request observaciones finales y consumo asociado
     * @return orden finalizada
     */
    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN','TECNICO')")
    public OrdenTrabajoResponse finalizeOrder(@PathVariable Long id,
                                              @Valid @RequestBody OrdenTrabajoFinalizeRequest request) {
        return ordenTrabajoService.finalizeOrder(id, request);
    }
}

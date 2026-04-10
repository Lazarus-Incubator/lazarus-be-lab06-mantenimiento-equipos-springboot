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
import pe.incubadora.backend.dto.incidencia.IncidenciaDecisionRequest;
import pe.incubadora.backend.dto.incidencia.IncidenciaRejectRequest;
import pe.incubadora.backend.dto.incidencia.IncidenciaRequest;
import pe.incubadora.backend.dto.incidencia.IncidenciaResponse;
import pe.incubadora.backend.dto.incidencia.IncidenciaUpdateRequest;
import pe.incubadora.backend.entity.EstadoIncidencia;
import pe.incubadora.backend.entity.PrioridadIncidencia;
import pe.incubadora.backend.entity.TipoIncidencia;
import pe.incubadora.backend.service.IncidenciaService;

/**
 * Controlador REST para el ciclo de vida de incidencias sobre equipos.
 *
 * <p>Aquí se centralizan tanto los listados como las transiciones visibles
 * para usuarios de sede y áreas operativas.</p>
 */
@RestController
@RequestMapping("/api/v1/incidencias")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    /**
     * Devuelve incidencias filtrables por sede, equipo y criterios de negocio.
     *
     * @return página de incidencias visibles para el usuario actual
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public Page<IncidenciaResponse> findAll(@RequestParam(required = false) Long sedeId,
                                            @RequestParam(required = false) Long equipoId,
                                            @RequestParam(required = false) EstadoIncidencia estado,
                                            @RequestParam(required = false) PrioridadIncidencia prioridad,
                                            @RequestParam(required = false) TipoIncidencia tipo,
                                            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return incidenciaService.findAll(sedeId, equipoId, estado, prioridad, tipo, pageable);
    }

    /**
     * Obtiene el detalle completo de una incidencia.
     *
     * @param id identificador interno de la incidencia
     * @return respuesta completa con datos del equipo, sede y trazabilidad
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public IncidenciaResponse findById(@PathVariable Long id) {
        return incidenciaService.findById(id);
    }

    /**
     * Registra una nueva incidencia reportada sobre un equipo.
     *
     * @param request datos mínimos para abrir el caso
     * @return incidencia creada y enriquecida por la capa de servicio
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public ResponseEntity<IncidenciaResponse> create(@Valid @RequestBody IncidenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.create(request));
    }

    /**
     * Modifica una incidencia existente mientras el flujo lo permita.
     *
     * @param id identificador de la incidencia
     * @param request cambios solicitados por el cliente
     * @return incidencia actualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public IncidenciaResponse update(@PathVariable Long id, @Valid @RequestBody IncidenciaUpdateRequest request) {
        return incidenciaService.update(id, request);
    }

    /**
     * Elimina incidencias que todavía pueden descartarse sin afectar el flujo.
     *
     * @param id identificador de la incidencia
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        incidenciaService.delete(id);
    }

    /**
     * Mueve una incidencia al estado de revisión operativa.
     *
     * @param id incidencia a revisar
     * @param request comentario opcional de la revisión
     * @return incidencia con el nuevo estado
     */
    @PostMapping("/{id}/en-revision")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public IncidenciaResponse moveToRevision(@PathVariable Long id,
                                             @Valid @RequestBody IncidenciaDecisionRequest request) {
        return incidenciaService.moveToRevision(id, request);
    }

    /**
     * Aprueba una incidencia ya evaluada para continuar con la atención.
     *
     * @param id incidencia a aprobar
     * @param request comentario asociado a la decisión
     * @return incidencia en su nuevo estado
     */
    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public IncidenciaResponse approve(@PathVariable Long id,
                                      @Valid @RequestBody IncidenciaDecisionRequest request) {
        return incidenciaService.approve(id, request);
    }

    /**
     * Rechaza una incidencia durante la fase de revisión.
     *
     * @param id incidencia evaluada
     * @param request motivo textual del rechazo
     * @return incidencia con la decisión aplicada
     */
    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES')")
    public IncidenciaResponse reject(@PathVariable Long id,
                                     @Valid @RequestBody IncidenciaRejectRequest request) {
        return incidenciaService.reject(id, request);
    }

    /**
     * Cierra un caso ya resuelto para concluir el flujo operativo.
     *
     * @param id incidencia a cerrar
     * @return incidencia cerrada
     */
    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACIONES','SEDE')")
    public IncidenciaResponse close(@PathVariable Long id) {
        return incidenciaService.close(id);
    }
}

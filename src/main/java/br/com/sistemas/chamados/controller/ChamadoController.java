package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ComentarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService service;
    private final ComentarioService comentarioService;

    public ChamadoController(ChamadoService service, ComentarioService comentarioService) {
        this.service = service;
        this.comentarioService = comentarioService;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> abrir(@Valid @RequestBody ChamadoRequest dto) {
        ChamadoResponse criado = service.abrir(dto);
        return ResponseEntity.created(URI.create("/chamados/" + criado.id())).body(criado);
    }

    /**
     * GET /chamados?status=ABERTO&prioridade=ALTA&clienteId=1&page=0&size=10&sort=dataAbertura,desc
     * Todos os filtros são opcionais.
     */
    @GetMapping
    public PaginaResponse<ChamadoResponse> listar(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long clienteId,
            @PageableDefault(size = 10, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(status, prioridade, clienteId, pageable);
    }

    /** Detalhe do chamado com os comentários. */
    @GetMapping("/{id}")
    public ChamadoDetalheResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ChamadoRequest dto) {
        return service.atualizar(id, dto);
    }

    @PatchMapping("/{id}/atribuir")
    public ChamadoResponse atribuir(@PathVariable Long id, @Valid @RequestBody AtribuirTecnicoRequest dto) {
        return service.atribuirTecnico(id, dto.tecnicoId());
    }

    @PatchMapping("/{id}/status")
    public ChamadoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusRequest dto) {
        return service.alterarStatus(id, dto.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(@PathVariable Long id,
                                                       @Valid @RequestBody ComentarioRequest dto) {
        ComentarioResponse criado = comentarioService.comentar(id, dto);
        return ResponseEntity.created(URI.create("/chamados/" + id + "/comentarios/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}/comentarios")
    public List<ComentarioResponse> listarComentarios(@PathVariable Long id) {
        return comentarioService.listar(id);
    }
}

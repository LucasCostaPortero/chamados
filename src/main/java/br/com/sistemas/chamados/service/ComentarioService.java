package br.com.sistemas.chamados.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.repository.ComentarioRepository;

@Service
public class ComentarioService {

    private final ComentarioRepository repository;
    private final ChamadoService chamadoService;

    public ComentarioService(ComentarioRepository repository, ChamadoService chamadoService) {
        this.repository = repository;
        this.chamadoService = chamadoService;
    }

    /** Regra: chamado fechado não recebe novos comentários. */
    @Transactional
    public ComentarioResponse comentar(Long chamadoId, ComentarioRequest dto) {
        Chamado chamado = chamadoService.buscarEntity(chamadoId);
        chamadoService.garantirNaoFechado(chamado);
        Comentario salvo = repository.save(new Comentario(dto.texto(), dto.autor(), chamado));
        return ComentarioResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> listar(Long chamadoId) {
        chamadoService.buscarEntity(chamadoId); // 404 se o chamado não existir
        return repository.findByChamadoIdOrderByDataAsc(chamadoId).stream()
            .map(ComentarioResponse::de)
            .toList();
    }
}

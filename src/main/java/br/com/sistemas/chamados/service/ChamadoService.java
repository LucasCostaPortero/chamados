package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.exception.TransicaoStatusInvalidaException;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final ComentarioRepository comentarioRepository;
    private final CategoriaService categoriaService;
    private final TecnicoService tecnicoService;

    public ChamadoService(ChamadoRepository repository,
                          ClienteRepository clienteRepository,
                          ComentarioRepository comentarioRepository,
                          CategoriaService categoriaService,
                          TecnicoService tecnicoService) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.comentarioRepository = comentarioRepository;
        this.categoriaService = categoriaService;
        this.tecnicoService = tecnicoService;
    }

    /** Abre um chamado. Status ABERTO e dataAbertura são definidos na entidade (@PrePersist). */
    @Transactional
    public ChamadoResponse abrir(ChamadoRequest dto) {
        Cliente cliente = buscarCliente(dto.clienteId());
        Categoria categoria = categoriaService.buscarEntity(dto.categoriaId());
        Chamado chamado = new Chamado(dto.titulo(), dto.descricao(), dto.prioridade(), cliente, categoria);
        return ChamadoResponse.de(repository.save(chamado));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResponse> listar(StatusChamado status, Prioridade prioridade,
                                                  Long clienteId, Pageable pageable) {
        return PaginaResponse.de(repository.filtrar(status, prioridade, clienteId, pageable), ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        buscarCliente(clienteId); // 404 se o cliente não existir
        return PaginaResponse.de(repository.findByClienteId(clienteId, pageable), ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse detalhar(Long id) {
        Chamado chamado = buscarEntity(id);
        var comentarios = comentarioRepository.findByChamadoIdOrderByDataAsc(id).stream()
            .map(ComentarioResponse::de)
            .toList();
        return new ChamadoDetalheResponse(ChamadoResponse.de(chamado), comentarios);
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);
        garantirNaoFechado(chamado);
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(categoriaService.buscarEntity(dto.categoriaId()));
        return ChamadoResponse.de(chamado);
    }

    /**
     * PATCH /chamados/{id}/atribuir
     * Regra: não é possível atribuir técnico a chamado fechado.
     * Se o chamado ainda estava ABERTO, passa automaticamente para EM_ATENDIMENTO.
     */
    @Transactional
    public ChamadoResponse atribuirTecnico(Long id, Long tecnicoId) {
        Chamado chamado = buscarEntity(id);
        if (chamado.isFechado()) {
            throw new RegraNegocioException("Não é possível atribuir técnico ao chamado " + id + " porque ele está FECHADO");
        }
        Tecnico tecnico = tecnicoService.buscarEntity(tecnicoId);
        chamado.setTecnico(tecnico);
        if (chamado.getStatus() == StatusChamado.ABERTO) {
            chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        }
        return ChamadoResponse.de(chamado);
    }

    /**
     * PATCH /chamados/{id}/status
     * Regras: chamado fechado não muda; só transições previstas em StatusChamado;
     * EM_ATENDIMENTO exige técnico; ao FECHAR preenche dataFechamento.
     */
    @Transactional
    public ChamadoResponse alterarStatus(Long id, StatusChamado novoStatus) {
        Chamado chamado = buscarEntity(id);
        garantirNaoFechado(chamado);
        StatusChamado atual = chamado.getStatus();
        if (!atual.podeMudarPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(
                "Transição de status inválida: " + atual + " -> " + novoStatus);
        }
        if (novoStatus == StatusChamado.EM_ATENDIMENTO && chamado.getTecnico() == null) {
            throw new RegraNegocioException("Atribua um técnico antes de colocar o chamado EM_ATENDIMENTO");
        }
        chamado.setStatus(novoStatus);
        if (novoStatus == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        Chamado chamado = buscarEntity(id);
        comentarioRepository.deleteByChamadoId(id);
        repository.delete(chamado);
    }

    /** Usado também pelo ComentarioService. */
    @Transactional(readOnly = true)
    public Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + id + " não encontrado"));
    }

    public void garantirNaoFechado(Chamado chamado) {
        if (chamado.isFechado()) {
            throw new RegraNegocioException("Chamado " + chamado.getId() + " está FECHADO e não pode ser alterado");
        }
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + clienteId + " não encontrado"));
    }
}

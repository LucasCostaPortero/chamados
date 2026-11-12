package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

@DataJpaTest
class ChamadoRepositoryTest {

    @Autowired
    private ChamadoRepository repository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Cliente ana;
    private Cliente bia;

    @BeforeEach
    void preparar() {
        ana = clienteRepository.save(new Cliente("Ana", "ana@exemplo.com", null));
        bia = clienteRepository.save(new Cliente("Bia", "bia@exemplo.com", null));
        Categoria rede = categoriaRepository.save(new Categoria("Rede", "Problemas de rede"));

        repository.save(new Chamado("Sem internet", "Wi-Fi caiu", Prioridade.ALTA, ana, rede));
        repository.save(new Chamado("Lentidão", "Rede lenta", Prioridade.BAIXA, ana, rede));
        Chamado fechado = new Chamado("Cabo", "Cabo rompido", Prioridade.ALTA, bia, rede);
        fechado.setStatus(StatusChamado.FECHADO);
        repository.save(fechado);
    }

    @Test
    void chamadoNasceAbertoComDataDeAbertura() {
        Categoria hw = categoriaRepository.save(new Categoria("Hardware", null));
        Chamado c = repository.save(new Chamado("Mouse", "Mouse parou", Prioridade.MEDIA, ana, hw));

        assertThat(c.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(c.getDataAbertura()).isNotNull();
    }

    @Test
    void filtrarSemParametrosTrazTodos() {
        Page<Chamado> page = repository.filtrar(null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void filtrarPorStatusPrioridadeECliente() {
        assertThat(repository.filtrar(StatusChamado.ABERTO, null, null, PageRequest.of(0, 10))
            .getTotalElements()).isEqualTo(2);
        assertThat(repository.filtrar(null, Prioridade.ALTA, null, PageRequest.of(0, 10))
            .getTotalElements()).isEqualTo(2);
        assertThat(repository.filtrar(StatusChamado.ABERTO, Prioridade.ALTA, ana.getId(), PageRequest.of(0, 10))
            .getTotalElements()).isEqualTo(1);
    }

    @Test
    void paginacaoRespeitaTamanho() {
        Page<Chamado> page = repository.filtrar(null, null, null, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void verificaSeClienteTemChamados() {
        assertThat(repository.existsByClienteId(bia.getId())).isTrue();
        Cliente semChamados = clienteRepository.save(new Cliente("Caio", "caio@exemplo.com", null));
        assertThat(repository.existsByClienteId(semChamados.getId())).isFalse();
    }
}

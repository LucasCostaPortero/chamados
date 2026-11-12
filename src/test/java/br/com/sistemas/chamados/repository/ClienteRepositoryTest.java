package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import br.com.sistemas.chamados.entity.Cliente;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    @Test
    void deveSalvarEGerarId() {
        Cliente salvo = repository.save(new Cliente("Ana Souza", "ana@exemplo.com", "47999999999"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId())).isPresent();
    }

    @Test
    void existsByEmailDeveDetectarDuplicidade() {
        repository.save(new Cliente("Ana Souza", "ana@exemplo.com", null));

        assertThat(repository.existsByEmail("ana@exemplo.com")).isTrue();
        assertThat(repository.existsByEmail("outro@exemplo.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotDeveIgnorarOProprioRegistro() {
        Cliente ana = repository.save(new Cliente("Ana Souza", "ana@exemplo.com", null));
        Cliente bia = repository.save(new Cliente("Bia Lima", "bia@exemplo.com", null));

        // Ana atualizando com o próprio e-mail: não é duplicidade
        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", ana.getId())).isFalse();
        // Bia tentando usar o e-mail da Ana: é duplicidade
        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", bia.getId())).isTrue();
    }
}

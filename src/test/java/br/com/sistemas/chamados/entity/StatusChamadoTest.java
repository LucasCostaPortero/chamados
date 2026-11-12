package br.com.sistemas.chamados.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Teste unitário puro (sem Spring) das transições de status. */
class StatusChamadoTest {

    @Test
    void transicoesValidas() {
        assertThat(StatusChamado.ABERTO.podeMudarPara(StatusChamado.EM_ATENDIMENTO)).isTrue();
        assertThat(StatusChamado.ABERTO.podeMudarPara(StatusChamado.FECHADO)).isTrue();
        assertThat(StatusChamado.EM_ATENDIMENTO.podeMudarPara(StatusChamado.AGUARDANDO_CLIENTE)).isTrue();
        assertThat(StatusChamado.AGUARDANDO_CLIENTE.podeMudarPara(StatusChamado.EM_ATENDIMENTO)).isTrue();
    }

    @Test
    void transicoesInvalidas() {
        assertThat(StatusChamado.EM_ATENDIMENTO.podeMudarPara(StatusChamado.ABERTO)).isFalse();
        assertThat(StatusChamado.ABERTO.podeMudarPara(StatusChamado.ABERTO)).isFalse();
        for (StatusChamado s : StatusChamado.values()) {
            assertThat(StatusChamado.FECHADO.podeMudarPara(s)).isFalse();
        }
    }
}

package br.com.sistemas.chamados.entity;

import java.util.Set;

/**
 * Ciclo de vida do chamado.
 *
 * Transições permitidas:
 *   ABERTO             -> EM_ATENDIMENTO, AGUARDANDO_CLIENTE, FECHADO
 *   EM_ATENDIMENTO     -> AGUARDANDO_CLIENTE, FECHADO
 *   AGUARDANDO_CLIENTE -> EM_ATENDIMENTO, FECHADO
 *   FECHADO            -> (nenhuma: chamado fechado não pode ser alterado)
 */
public enum StatusChamado {
    ABERTO,
    EM_ATENDIMENTO,
    AGUARDANDO_CLIENTE,
    FECHADO;

    public boolean podeMudarPara(StatusChamado novo) {
        return transicoesPermitidas().contains(novo);
    }

    private Set<StatusChamado> transicoesPermitidas() {
        return switch (this) {
            case ABERTO -> Set.of(EM_ATENDIMENTO, AGUARDANDO_CLIENTE, FECHADO);
            case EM_ATENDIMENTO -> Set.of(AGUARDANDO_CLIENTE, FECHADO);
            case AGUARDANDO_CLIENTE -> Set.of(EM_ATENDIMENTO, FECHADO);
            case FECHADO -> Set.of();
        };
    }
}

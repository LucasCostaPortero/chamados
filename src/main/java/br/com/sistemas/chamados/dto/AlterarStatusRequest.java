package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.StatusChamado;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(
    @NotNull(message = "Status é obrigatório (ABERTO, EM_ATENDIMENTO, AGUARDANDO_CLIENTE ou FECHADO)")
    StatusChamado status
) {}

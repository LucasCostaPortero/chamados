package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

/** Saída resumida do chamado, usada nas listagens. */
public record ChamadoResponse(
    Long id,
    String titulo,
    String descricao,
    LocalDateTime dataAbertura,
    LocalDateTime dataFechamento,
    Prioridade prioridade,
    StatusChamado status,
    Long clienteId,
    String clienteNome,
    Long categoriaId,
    String categoriaNome,
    Long tecnicoId,
    String tecnicoNome
) {
    public static ChamadoResponse de(Chamado c) {
        return new ChamadoResponse(
            c.getId(),
            c.getTitulo(),
            c.getDescricao(),
            c.getDataAbertura(),
            c.getDataFechamento(),
            c.getPrioridade(),
            c.getStatus(),
            c.getCliente().getId(),
            c.getCliente().getNome(),
            c.getCategoria().getId(),
            c.getCategoria().getNome(),
            c.getTecnico() != null ? c.getTecnico().getId() : null,
            c.getTecnico() != null ? c.getTecnico().getNome() : null
        );
    }
}

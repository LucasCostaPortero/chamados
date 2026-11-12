package br.com.sistemas.chamados.dto;

import java.util.List;

/** Detalhe do chamado: dados do chamado + comentários em ordem cronológica. */
public record ChamadoDetalheResponse(
    ChamadoResponse chamado,
    List<ComentarioResponse> comentarios
) {}

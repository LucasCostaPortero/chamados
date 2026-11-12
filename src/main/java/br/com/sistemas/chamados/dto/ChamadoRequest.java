package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada para abrir (POST) ou editar (PUT) um chamado.
 * Status e técnico NÃO entram aqui: mudam só pelas rotas PATCH de ação.
 */
public record ChamadoRequest(
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 150, message = "Título deve ter no máximo 150 caracteres")
    String titulo,

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
    String descricao,

    @NotNull(message = "Prioridade é obrigatória (BAIXA, MEDIA, ALTA ou CRITICA)")
    Prioridade prioridade,

    @NotNull(message = "clienteId é obrigatório")
    Long clienteId,

    @NotNull(message = "categoriaId é obrigatório")
    Long categoriaId
) {}

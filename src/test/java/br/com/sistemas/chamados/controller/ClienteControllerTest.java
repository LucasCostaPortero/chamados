package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ClienteService;

/** Testa só a camada web: o Service é substituído por um mock. */
@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService service;

    private static final String JSON_VALIDO = """
        {"nome":"Ana Souza","email":"ana@exemplo.com","telefone":"47999999999"}
        """;

    @Test
    void criarRetorna201ComLocation() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", "47999999999"));

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/clientes/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
    }

    @Test
    void criarComDadosInvalidosRetorna400() throws Exception {
        String invalido = """
            {"nome":"","email":"email-invalido","telefone":"123"}
            """;

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(invalido))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.detalhes.length()").value(2));
    }

    @Test
    void criarComEmailDuplicadoRetorna409() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void listarRetorna200ComArray() throws Exception {
        when(service.listar()).thenReturn(List.of(
            new ClienteResponse(1L, "Ana", "ana@exemplo.com", null),
            new ClienteResponse(2L, "Bia", "bia@exemplo.com", null)));

        mvc.perform(get("/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarIdInexistenteRetorna404() throws Exception {
        when(service.buscar(eq(99999L)))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 99999 não encontrado"));

        mvc.perform(get("/clientes/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Cliente 99999 não encontrado"));
    }

    @Test
    void excluirRetorna204() throws Exception {
        mvc.perform(delete("/clientes/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void excluirInexistenteRetorna404() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Cliente 5 não encontrado")).when(service).excluir(5L);

        mvc.perform(delete("/clientes/5"))
            .andExpect(status().isNotFound());
    }
}

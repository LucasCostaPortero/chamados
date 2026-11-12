# Sistema de Chamados — API REST

Projeto da disciplina **DAS — Design e Arquitetura de Sistemas** (UNIVILLE).
Java 21 · Spring Boot 4.1.1 · Spring Web · Spring Data JPA · H2 · Bean Validation.

Arquitetura em camadas: `controller → service → repository → entity`, com `dto` para entrada/saída
e `exception` para padronizar os erros HTTP.

## Como executar

```bash
./mvnw spring-boot:run        # macOS / Linux
.\mvnw spring-boot:run        # Windows PowerShell
./mvnw test                   # testes automatizados
```

- API: `http://localhost:8080`
- Console H2: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:chamadosdb`, usuário `sa`, senha em branco)

## Rotas

| Recurso | Método | Rota | Sucesso |
|---|---|---|---|
| Clientes | POST / GET | `/clientes` | 201 / 200 |
| | GET / PUT / DELETE | `/clientes/{id}` | 200 / 200 / 204 |
| | GET | `/clientes/{id}/chamados` | 200 (paginado) |
| Categorias | POST / GET | `/categorias` | 201 / 200 |
| | GET / PUT / DELETE | `/categorias/{id}` | 200 / 200 / 204 |
| Técnicos | POST / GET | `/tecnicos` | 201 / 200 |
| | GET / PUT / DELETE | `/tecnicos/{id}` | 200 / 200 / 204 |
| Chamados | POST | `/chamados` | 201 |
| | GET | `/chamados?status=&prioridade=&clienteId=&page=&size=&sort=` | 200 (paginado) |
| | GET | `/chamados/{id}` (detalhe + comentários) | 200 |
| | PUT / DELETE | `/chamados/{id}` | 200 / 204 |
| | PATCH | `/chamados/{id}/atribuir` | 200 |
| | PATCH | `/chamados/{id}/status` | 200 |
| Comentários | POST / GET | `/chamados/{id}/comentarios` | 201 / 200 |

Erros padronizados: **400** dados inválidos · **404** recurso inexistente · **409** regra de negócio violada.

## Regras de negócio

- E-mail de cliente e de técnico é único; nome de categoria é único (sem diferenciar maiúsculas).
- Todo chamado nasce `ABERTO` e com `dataAbertura` preenchida automaticamente.
- Transições de status permitidas:
  `ABERTO → EM_ATENDIMENTO | AGUARDANDO_CLIENTE | FECHADO`,
  `EM_ATENDIMENTO → AGUARDANDO_CLIENTE | FECHADO`,
  `AGUARDANDO_CLIENTE → EM_ATENDIMENTO | FECHADO`.
- Chamado `FECHADO` não pode ser editado, receber técnico, mudar de status nem receber comentário.
- Ao fechar, `dataFechamento` é preenchida.
- Atribuir técnico a um chamado `ABERTO` o coloca em `EM_ATENDIMENTO`; `EM_ATENDIMENTO` exige técnico.
- Cliente, categoria ou técnico com chamados vinculados não pode ser excluído (409).

## Exemplos (curl)

```bash
# Cliente, categoria e técnico
curl -i -X POST http://localhost:8080/clientes -H "Content-Type: application/json" \
  -d '{"nome":"Ana Souza","email":"ana@exemplo.com","telefone":"47999999999"}'
curl -i -X POST http://localhost:8080/categorias -H "Content-Type: application/json" \
  -d '{"nome":"Rede","descricao":"Problemas de conexão"}'
curl -i -X POST http://localhost:8080/tecnicos -H "Content-Type: application/json" \
  -d '{"nome":"Carlos","email":"carlos@suporte.com","perfil":"TECNICO"}'

# Abrir chamado
curl -i -X POST http://localhost:8080/chamados -H "Content-Type: application/json" \
  -d '{"titulo":"Sem internet","descricao":"Wi-Fi caiu no setor","prioridade":"ALTA","clienteId":1,"categoriaId":1}'

# Atribuir técnico e mudar status
curl -i -X PATCH http://localhost:8080/chamados/1/atribuir -H "Content-Type: application/json" -d '{"tecnicoId":1}'
curl -i -X PATCH http://localhost:8080/chamados/1/status   -H "Content-Type: application/json" -d '{"status":"FECHADO"}'

# Comentar
curl -i -X POST http://localhost:8080/chamados/1/comentarios -H "Content-Type: application/json" \
  -d '{"texto":"Roteador reiniciado","autor":"Carlos"}'

# Filtros e paginação
curl -i "http://localhost:8080/chamados?status=ABERTO&prioridade=ALTA&page=0&size=5&sort=dataAbertura,desc"
curl -i "http://localhost:8080/clientes/1/chamados"

# Erros esperados
curl -i http://localhost:8080/clientes/99999                                  # 404
curl -i -X PATCH http://localhost:8080/chamados/1/status -H "Content-Type: application/json" \
  -d '{"status":"ABERTO"}'                                                    # 409 (chamado fechado)
```

## Testes automatizados

- `ClienteRepositoryTest` e `ChamadoRepositoryTest` (`@DataJpaTest`): persistência, duplicidade, filtros e paginação.
- `ClienteControllerTest` (`@WebMvcTest`): 201, 400, 404, 409 e 204.
- `StatusChamadoTest`: transições de status válidas e inválidas.

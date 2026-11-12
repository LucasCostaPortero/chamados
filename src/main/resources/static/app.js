// Front-end simples do Sistema de Chamados: consome a API REST do próprio servidor.

const $ = (sel) => document.querySelector(sel);
const STATUS = ['ABERTO', 'EM_ATENDIMENTO', 'AGUARDANDO_CLIENTE', 'FECHADO'];
const PRIORIDADES = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'];

// ---------- utilitários ----------

function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function selo(valor) {
  return `<span class="selo ${esc(valor)}">${esc(valor)}</span>`;
}

function dataBr(iso) {
  return iso ? new Date(iso).toLocaleString('pt-BR') : '-';
}

let timerAviso;
function avisar(msg, tipo = 'ok') {
  const el = $('#aviso');
  el.textContent = msg;
  el.className = `aviso ${tipo}`;
  clearTimeout(timerAviso);
  timerAviso = setTimeout(() => el.classList.add('oculto'), 4000);
}

/** Chama a API e transforma o ErroResposta do back-end em mensagem legível. */
async function api(metodo, url, corpo) {
  const opcoes = { method: metodo, headers: {} };
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }
  const resp = await fetch(url, opcoes);
  if (resp.status === 204) return null;
  const dados = await resp.json().catch(() => null);
  if (!resp.ok) {
    const detalhes = dados?.detalhes?.length ? ' — ' + dados.detalhes.join('; ') : '';
    throw new Error(`${resp.status}: ${dados?.erro ?? 'Erro inesperado'}${detalhes}`);
  }
  return dados;
}

async function tentar(acao, msgOk) {
  try {
    await acao();
    if (msgOk) avisar(msgOk);
  } catch (e) {
    avisar(e.message, 'erro');
  }
}

function dadosDoForm(form) {
  const dados = Object.fromEntries(new FormData(form).entries());
  for (const k of Object.keys(dados)) if (dados[k] === '') dados[k] = null;
  return dados;
}

// ---------- navegação ----------

document.querySelectorAll('.aba').forEach((botao) => {
  botao.addEventListener('click', () => mostrarTela(botao.dataset.tela));
});

function mostrarTela(nome) {
  document.querySelectorAll('.aba').forEach((b) => b.classList.toggle('ativa', b.dataset.tela === nome));
  document.querySelectorAll('.tela').forEach((t) => t.classList.toggle('oculto', t.id !== `tela-${nome}`));
  if (nome === 'dashboard') carregarDashboard();
  if (nome === 'chamados') { carregarCombos(); carregarChamados(); }
  if (CADASTROS[nome]) carregarCadastro(nome);
}

// ---------- dashboard ----------

async function contar(params) {
  const pagina = await api('GET', `/chamados?size=1&${params}`);
  return pagina.totalElementos;
}

async function carregarDashboard() {
  await tentar(async () => {
    const porStatus = await Promise.all(STATUS.map((s) => contar(`status=${s}`)));
    $('#cards-status').innerHTML = STATUS.map((s, i) =>
      `<div class="card">${selo(s)}<strong>${porStatus[i]}</strong></div>`).join('');
    const porPrioridade = await Promise.all(PRIORIDADES.map((p) => contar(`prioridade=${p}`)));
    $('#cards-prioridade').innerHTML = PRIORIDADES.map((p, i) =>
      `<div class="card">${selo(p)}<strong>${porPrioridade[i]}</strong></div>`).join('');
  });
}

// ---------- cadastros simples (clientes, categorias, técnicos) ----------

const CADASTROS = {
  clientes: { url: '/clientes', colunas: (c) => [c.id, c.nome, c.email, c.telefone ?? ''] },
  categorias: { url: '/categorias', colunas: (c) => [c.id, c.nome, c.descricao ?? ''] },
  tecnicos: { url: '/tecnicos', colunas: (t) => [t.id, t.nome, t.email, t.perfil] },
};

async function carregarCadastro(nome) {
  const cfg = CADASTROS[nome];
  await tentar(async () => {
    const itens = await api('GET', cfg.url);
    $(`#lista-${nome}`).innerHTML = itens.length
      ? itens.map((item) => `<tr>
          ${cfg.colunas(item).map((v) => `<td>${esc(v)}</td>`).join('')}
          <td>
            <button class="mini secundario" data-editar="${item.id}">Editar</button>
            <button class="mini perigo" data-excluir="${item.id}">Excluir</button>
          </td></tr>`).join('')
      : '<tr><td colspan="5" class="muted">Nenhum registro.</td></tr>';

    $(`#lista-${nome}`).querySelectorAll('[data-editar]').forEach((b) =>
      b.addEventListener('click', () => preencherForm(nome, itens.find((i) => i.id == b.dataset.editar))));
    $(`#lista-${nome}`).querySelectorAll('[data-excluir]').forEach((b) =>
      b.addEventListener('click', () => excluirCadastro(nome, b.dataset.excluir)));
  });
}

function preencherForm(nome, item) {
  const form = $(`#form-${nome}`);
  for (const [k, v] of Object.entries(item)) {
    if (form.elements[k]) form.elements[k].value = v ?? '';
  }
}

async function excluirCadastro(nome, id) {
  if (!confirm(`Excluir registro ${id}?`)) return;
  await tentar(async () => {
    await api('DELETE', `${CADASTROS[nome].url}/${id}`);
    await carregarCadastro(nome);
  }, 'Registro excluído');
}

Object.keys(CADASTROS).forEach((nome) => {
  const form = $(`#form-${nome}`);
  // campo hidden não é limpo pelo reset nativo; sem isso "Limpar" manteria o modo edição
  form.addEventListener('reset', () => setTimeout(() => { form.elements.id.value = ''; }));
  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const { id, ...corpo } = dadosDoForm(form);
    await tentar(async () => {
      if (id) await api('PUT', `${CADASTROS[nome].url}/${id}`, corpo);
      else await api('POST', CADASTROS[nome].url, corpo);
      form.reset();
      form.elements.id.value = '';
      await carregarCadastro(nome);
    }, id ? 'Registro atualizado' : 'Registro criado');
  });
});

// ---------- chamados ----------

let paginaAtual = 0;
let totalPaginas = 1;
let chamadoSelecionado = null;

async function carregarCombos() {
  await tentar(async () => {
    const [clientes, categorias, tecnicos] = await Promise.all([
      api('GET', '/clientes'), api('GET', '/categorias'), api('GET', '/tecnicos')]);
    const preencher = (sel, itens) => document.querySelectorAll(sel).forEach((s) => {
      const atual = s.value;
      const primeira = s.options[0].outerHTML;
      s.innerHTML = primeira + itens.map((i) => `<option value="${i.id}">${esc(i.nome)}</option>`).join('');
      s.value = atual;
    });
    preencher('.sel-clientes', clientes);
    preencher('.sel-categorias', categorias);
    preencher('.sel-tecnicos', tecnicos);
  });
}

async function carregarChamados() {
  const filtros = new URLSearchParams();
  for (const [k, v] of new FormData($('#form-filtro'))) if (v) filtros.set(k, v);
  filtros.set('page', paginaAtual);
  filtros.set('size', 10);

  await tentar(async () => {
    const pagina = await api('GET', `/chamados?${filtros}`);
    totalPaginas = Math.max(pagina.totalPaginas, 1);
    $('#lista-chamados').innerHTML = pagina.conteudo.length
      ? pagina.conteudo.map((c) => `<tr class="clicavel" data-id="${c.id}">
          <td>${c.id}</td><td>${esc(c.titulo)}</td><td>${esc(c.clienteNome)}</td>
          <td>${selo(c.prioridade)}</td><td>${selo(c.status)}</td><td>${esc(c.tecnicoNome ?? '-')}</td>
        </tr>`).join('')
      : '<tr><td colspan="6" class="muted">Nenhum chamado encontrado.</td></tr>';
    $('#lista-chamados').querySelectorAll('tr[data-id]').forEach((tr) =>
      tr.addEventListener('click', () => abrirDetalhe(tr.dataset.id)));
    $('#pag-info').textContent = `Página ${paginaAtual + 1} de ${totalPaginas} (${pagina.totalElementos} chamados)`;
    $('#pag-anterior').disabled = paginaAtual === 0;
    $('#pag-proxima').disabled = paginaAtual + 1 >= totalPaginas;
  });
}

$('#form-filtro').addEventListener('submit', (ev) => { ev.preventDefault(); paginaAtual = 0; carregarChamados(); });
$('#pag-anterior').addEventListener('click', () => { paginaAtual--; carregarChamados(); });
$('#pag-proxima').addEventListener('click', () => { paginaAtual++; carregarChamados(); });

$('#form-chamado').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const corpo = dadosDoForm(ev.target);
  corpo.clienteId = Number(corpo.clienteId);
  corpo.categoriaId = Number(corpo.categoriaId);
  await tentar(async () => {
    const criado = await api('POST', '/chamados', corpo);
    ev.target.reset();
    await carregarChamados();
    await abrirDetalhe(criado.id);
  }, 'Chamado aberto');
});

async function abrirDetalhe(id) {
  await tentar(async () => {
    const { chamado: c, comentarios } = await api('GET', `/chamados/${id}`);
    chamadoSelecionado = c;
    $('#detalhe').classList.remove('oculto');
    $('#det-titulo').innerHTML = `#${c.id} ${esc(c.titulo)} ${selo(c.status)}`;
    $('#det-info').innerHTML = `${selo(c.prioridade)} · Cliente: ${esc(c.clienteNome)} · Categoria: ${esc(c.categoriaNome)}
      · Técnico: ${esc(c.tecnicoNome ?? 'não atribuído')}<br>
      Aberto em ${dataBr(c.dataAbertura)}${c.dataFechamento ? ' · Fechado em ' + dataBr(c.dataFechamento) : ''}`;
    $('#det-descricao').textContent = c.descricao;
    $('#det-tecnico').value = c.tecnicoId ?? '';
    $('#det-comentarios').innerHTML = comentarios.length
      ? comentarios.map((cm) => `<li><strong>${esc(cm.autor)}</strong>
          <span class="muted">${dataBr(cm.data)}</span><br>${esc(cm.texto)}</li>`).join('')
      : '<li class="muted">Sem comentários.</li>';

    const fechado = c.status === 'FECHADO';
    ['#btn-atribuir', '#btn-status', '#det-tecnico', '#det-status'].forEach((s) => { $(s).disabled = fechado; });
    $('#form-comentario').classList.toggle('oculto', fechado);
  });
}

$('#btn-atribuir').addEventListener('click', async () => {
  const tecnicoId = Number($('#det-tecnico').value);
  if (!tecnicoId) return avisar('Selecione um técnico', 'erro');
  await tentar(async () => {
    await api('PATCH', `/chamados/${chamadoSelecionado.id}/atribuir`, { tecnicoId });
    await abrirDetalhe(chamadoSelecionado.id);
    await carregarChamados();
  }, 'Técnico atribuído');
});

$('#btn-status').addEventListener('click', async () => {
  await tentar(async () => {
    await api('PATCH', `/chamados/${chamadoSelecionado.id}/status`, { status: $('#det-status').value });
    await abrirDetalhe(chamadoSelecionado.id);
    await carregarChamados();
  }, 'Status alterado');
});

$('#form-comentario').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  await tentar(async () => {
    await api('POST', `/chamados/${chamadoSelecionado.id}/comentarios`, dadosDoForm(ev.target));
    ev.target.elements.texto.value = '';
    await abrirDetalhe(chamadoSelecionado.id);
  }, 'Comentário registrado');
});

$('#btn-excluir-chamado').addEventListener('click', async () => {
  if (!confirm(`Excluir o chamado #${chamadoSelecionado.id}?`)) return;
  await tentar(async () => {
    await api('DELETE', `/chamados/${chamadoSelecionado.id}`);
    $('#detalhe').classList.add('oculto');
    chamadoSelecionado = null;
    await carregarChamados();
  }, 'Chamado excluído');
});

// tela inicial
mostrarTela('dashboard');

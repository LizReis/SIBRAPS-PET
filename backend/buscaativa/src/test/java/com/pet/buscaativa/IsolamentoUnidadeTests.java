package com.pet.buscaativa;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pet.buscaativa.entities.*;
import com.pet.buscaativa.entities.enums.*;
import com.pet.buscaativa.repositories.*;
import com.pet.buscaativa.services.TokenService;
import com.pet.buscaativa.services.PacienteService;
import com.pet.buscaativa.services.PoliticaClassificacaoPaciente;
import com.pet.buscaativa.entities.dto.PacienteDTO;
import com.pet.buscaativa.services.exceptions.RecursoDuplicadoException;
import com.pet.buscaativa.services.impl.SusApiAdapter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Exercita o filtro JWT, controllers, services e consultas reais no H2. */
@SpringBootTest(properties = "logging.level.org.springframework.security=WARN")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class IsolamentoUnidadeTests {
    @MockitoBean SusApiAdapter susApi;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TokenService tokens;
    @Autowired PacienteService pacienteService;
    @Autowired PoliticaClassificacaoPaciente politica;
    @Autowired UsuarioRepository usuarios;
    @Autowired PacienteRepository pacientes;
    @Autowired UsfReferenciaRepository usfs;
    @Autowired AgendamentoRepository agendas;
    @Autowired GrupoTerapeuticoRepository grupos;
    @Autowired SessaoGrupoRepository sessoes;
    @Autowired SessaoGrupoParticipanteRepository participantes;

    Usuario ad, profissionalAd, recepcaoAd, capsI, usfA, usfB, legado;
    Usuario coordenadorAd, coordenadorI;
    Paciente pAd, pAdB, pI, pIA, pII;
    SessaoGrupo futura, realizada, estrangeira;
    Agendamento agendaAd, agendaI;

    @BeforeEach
    void preparar() {
        UsfReferencia a = usf("São Benedito");
        UsfReferencia b = usf("Andaiá");
        ad = usuario(UnidadeAtuacao.CAPS_AD, null, TipoUsuario.ADMINISTRADOR);
        profissionalAd = usuario(UnidadeAtuacao.CAPS_AD, null, TipoUsuario.PROFISSIONAL);
        recepcaoAd = usuario(UnidadeAtuacao.CAPS_AD, null, TipoUsuario.RECEPCAO);
        capsI = usuario(UnidadeAtuacao.CAPS_I, null, TipoUsuario.ADMINISTRADOR);
        usfA = usuario(UnidadeAtuacao.USF, a, TipoUsuario.ADMINISTRADOR);
        usfB = usuario(UnidadeAtuacao.USF, b, TipoUsuario.RECEPCAO);
        legado = usuario(UnidadeAtuacao.USF, null, TipoUsuario.ADMINISTRADOR);
        coordenadorAd = profissionalAd;
        coordenadorI = usuario(UnidadeAtuacao.CAPS_I, null, TipoUsuario.PROFISSIONAL);
        pAd = paciente("AD A", CapsEnum.CAPS_AD, a, 1);
        pAdB = paciente("AD B", CapsEnum.CAPS_AD, b, 2);
        pI = paciente("I B", CapsEnum.CAPS_I, b, 3);
        pIA = paciente("I A", CapsEnum.CAPS_I, a, 4);
        pII = paciente("II B", CapsEnum.CAPS_II, b, 5);
        // Uma referência de outra unidade nunca substitui capsReferencia/usfReferencia.
        pAd.setProfissionalReferencia(coordenadorI);
        pacientes.saveAndFlush(pAd);
        agendaAd = agenda(pAd, coordenadorAd, SituacaoAtendimento.AGENDADO, LocalDate.now());
        agendaI = agenda(pI, coordenadorI, SituacaoAtendimento.AGENDADO, LocalDate.now());
        agenda(pAdB, coordenadorAd, SituacaoAtendimento.FALTOU, LocalDate.now().minusDays(1));
        GrupoTerapeutico local = grupo(coordenadorAd, "Local");
        GrupoTerapeutico outro = grupo(coordenadorI, "Outra unidade");
        futura = sessao(local, LocalDate.now().plusDays(5), StatusSessaoGrupo.AGENDADA);
        realizada = sessao(local, LocalDate.now().minusDays(2), StatusSessaoGrupo.REALIZADA);
        estrangeira = sessao(outro, LocalDate.now().plusDays(5), StatusSessaoGrupo.AGENDADA);
        participante(futura, pAd, StatusPresencaGrupo.NAO_REGISTRADA);
        // Um grupo legado misto deve ocultar nomes e contagens de fora do escopo.
        participante(realizada, pAd, StatusPresencaGrupo.PRESENTE);
        participante(realizada, pI, StatusPresencaGrupo.PRESENTE);
        participante(estrangeira, pI, StatusPresencaGrupo.NAO_REGISTRADA);
    }

    private UsfReferencia usf(String nome) {
        var usf = new UsfReferencia();
        usf.setCnes(UUID.randomUUID().toString());
        usf.setNomeUsf("USF " + nome);
        return usfs.saveAndFlush(usf);
    }

    private Usuario usuario(UnidadeAtuacao unidade, UsfReferencia usf, TipoUsuario role) {
        var u = new Usuario();
        u.setNome("Isolamento usuário " + UUID.randomUUID());
        u.setEmail(UUID.randomUUID() + "@test.local");
        u.setSenha("senha-nao-utilizada-pelo-jwt");
        u.setTipoUsuario(role);
        u.setUnidadeAtuacao(unidade);
        u.setUsfAtuacao(usf);
        return usuarios.saveAndFlush(u);
    }

    private Paciente paciente(String nome, CapsEnum caps, UsfReferencia usf, int documento) {
        var p = new Paciente();
        p.setNome("Isolamento " + nome);
        p.setNomeMae("Mãe sintética");
        p.setDataNascimento(LocalDate.now().minusYears(30));
        p.setCpf("9000000000" + documento);
        p.setCns("90000000000000" + documento);
        p.setTelefone("00000000000");
        p.setTipoAcompanhamento(TipoAcompanhamento.INDIVIDUAL);
        p.setStatusPaciente(StatusPaciente.ATIVO);
        p.setClassificacaoRisco(ClassificacaoRisco.VERMELHO);
        p.setCountFaltas(3);
        p.setDataUltimaPresenca(LocalDate.now().minusDays(150));
        p.setCapsReferencia(caps);
        p.setUsfReferencia(usf);
        return pacientes.saveAndFlush(p);
    }

    private Agendamento agenda(Paciente p, Usuario u, SituacaoAtendimento status, LocalDate data) {
        var a = new Agendamento();
        a.setPaciente(p); a.setUsuario(u); a.setSituacaoAtendimento(status);
        a.setDataAgendamento(data); a.setHoraAtendimento(LocalTime.of(8, 0)); a.setTurnoAgendamento(TurnoEnum.MANHA);
        return agendas.saveAndFlush(a);
    }

    private GrupoTerapeutico grupo(Usuario u, String tema) {
        var g = new GrupoTerapeutico();
        g.setTema("Isolamento " + tema); g.setCoordenador(u);
        g.setRecorrencia(RecorrenciaGrupo.UNICA); g.setHorarioPadrao(LocalTime.of(9, 0));
        return grupos.saveAndFlush(g);
    }

    private SessaoGrupo sessao(GrupoTerapeutico g, LocalDate data, StatusSessaoGrupo status) {
        var s = new SessaoGrupo();
        s.setGrupo(g); s.setDataSessao(data); s.setHorario(LocalTime.of(9, 0)); s.setStatus(status);
        return sessoes.saveAndFlush(s);
    }

    private void participante(SessaoGrupo s, Paciente p, StatusPresencaGrupo status) {
        var item = new SessaoGrupoParticipante();
        item.setSessaoGrupo(s); item.setPaciente(p); item.setStatusPresenca(status);
        participantes.saveAndFlush(item);
        s.getParticipantes().add(item);
    }

    private MockHttpServletResponse getAs(Usuario usuario, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + tokens.gerarToken(usuario)))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse postAs(Usuario usuario, String url, String body) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + tokens.gerarToken(usuario))
                .contentType("application/json").content(body)).andReturn().getResponse();
    }

    private JsonNode ok(Usuario usuario, String url) throws Exception {
        var response = getAs(usuario, url);
        assertEquals(200, response.getStatus(), url);
        return json.readTree(response.getContentAsString());
    }

    private List<String> nomes(JsonNode lista) {
        return lista.valueStream().map(p -> p.get("nome").asString()).filter(n -> n.startsWith("Isolamento ")).sorted().toList();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void unidadeDeterminaPacientesSemUsarProfissionalDeReferencia(int indice) throws Exception {
        Usuario usuario = List.of(ad, capsI, usfA, usfB).get(indice);
        List<List<String>> esperados = List.of(
                List.of(pAd.getNome(), pAdB.getNome()),
                List.of(pIA.getNome(), pI.getNome()),
                List.of(pAd.getNome(), pIA.getNome()),
                List.of(pAdB.getNome(), pI.getNome(), pII.getNome()));
        var resultado = ok(usuario, "/api/pacientes/pesquisa?q=Isolamento&size=50");
        assertEquals(esperados.get(indice).stream().sorted().toList(), nomes(resultado.get("pacientes")));
        for (Paciente p : List.of(pAd, pAdB, pI, pIA, pII)) {
            boolean permitido = esperados.get(indice).contains(p.getNome());
            assertEquals(permitido ? 200 : 404, getAs(usuario, "/api/pacientes/" + p.getIdPublico()).getStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void rolesNaoConcedemBypassGlobal(int indice) throws Exception {
        Usuario usuario = List.of(ad, profissionalAd, recepcaoAd).get(indice);
        assertEquals(List.of(pAd.getNome(), pAdB.getNome()).stream().sorted().toList(),
                nomes(ok(usuario, "/api/pacientes/pesquisa?q=Isolamento").get("pacientes")));
        assertEquals(404, getAs(usuario, "/api/pacientes/" + pI.getIdPublico()).getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/detalhe", "/historico", "/agendamentos"})
    void uuidPermiteSomentePacientesDaUnidade(String sufixo) throws Exception {
        assertEquals(200, getAs(ad, "/api/pacientes/" + pAd.getIdPublico() + sufixo).getStatus());
        var fora = getAs(ad, "/api/pacientes/" + pI.getIdPublico() + sufixo);
        var ausente = getAs(ad, "/api/pacientes/" + UUID.randomUUID() + sufixo);
        assertEquals(404, fora.getStatus());
        assertEquals(404, ausente.getStatus());
        assertTrue(json.readTree(fora.getContentAsString()).get("message").asString()
                .startsWith("Resource not found. Id "));
        assertFalse(fora.getContentAsString().contains(pI.getNome()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cpf", "cns"})
    void buscaDiretaPorDocumentoNaoRevelaExistencia(String campo) throws Exception {
        String proprio = campo.equals("cpf") ? pAd.getCpf() : pAd.getCns();
        String outro = campo.equals("cpf") ? pI.getCpf() : pI.getCns();
        assertEquals(pAd.getNome(), ok(ad, "/api/pacientes/busca/" + campo + "/" + proprio).get("nome").asString());
        var fora = getAs(ad, "/api/pacientes/busca/" + campo + "/" + outro);
        var ausente = getAs(ad, "/api/pacientes/busca/" + campo + "/111111111111111");
        assertEquals(404, fora.getStatus());
        assertEquals(404, ausente.getStatus());
        assertEquals(json.readTree(ausente.getContentAsString()).get("message"),
                json.readTree(fora.getContentAsString()).get("message"));
        assertEquals(0, ok(ad, "/api/pacientes/pesquisa?q=" + outro).get("totalRegistros").asInt());
        assertEquals(1, ok(ad, "/api/pacientes/pesquisa?q=" + proprio).get("totalRegistros").asInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/pacientes", "/api/pacientes/busca/nome?q=Isolamento", "/api/pacientes/busca-ativa"})
    void listasNaoRetornamDadosDeOutraUnidade(String url) throws Exception {
        var r = getAs(ad, url);
        assertEquals(200, r.getStatus());
        assertFalse(r.getContentAsString().contains(pI.getNome()));
        assertFalse(r.getContentAsString().contains(pII.getNome()));
        assertFalse(r.getContentAsString().contains(pIA.getNome()));
        assertTrue(r.getContentAsString().contains(pAd.getNome()));
    }

    @Test
    void resumoDaListaEInicioUsamApenasPacientesEAtendimentosAutorizados() throws Exception {
        long ativos = pacientes.findAll().stream().filter(p -> p.getStatusPaciente() == StatusPaciente.ATIVO
                && p.getCapsReferencia() == CapsEnum.CAPS_AD).count();
        long atencao = pacientes.findAll().stream().filter(p -> p.getStatusPaciente() == StatusPaciente.ATIVO
                && p.getCapsReferencia() == CapsEnum.CAPS_AD && p.getClassificacaoRisco() != ClassificacaoRisco.VERDE).count();
        var lista = ok(ad, "/api/pacientes/pesquisa?q=Isolamento").get("resumo");
        assertEquals(ativos, lista.get("pacientesAtivos").asLong());
        var permitidos = pacientes.findAll().stream().filter(p -> p.getStatusPaciente() == StatusPaciente.ATIVO
                && p.getCapsReferencia() == CapsEnum.CAPS_AD).toList();
        assertEquals(permitidos.stream().filter(p -> p.getClassificacaoRisco() == ClassificacaoRisco.AMARELO).count(), lista.get("emAtencao").asLong());
        assertEquals(permitidos.stream().filter(p -> p.getClassificacaoRisco() == ClassificacaoRisco.VERMELHO).count(), lista.get("buscaAtiva").asLong());
        assertEquals(permitidos.stream().filter(p -> politica.semPresencaRecente(p, LocalDate.now())).count(), lista.get("semPresencaRecente").asLong());
        var inicio = ok(ad, "/api/inicio/resumo");
        assertEquals(ativos, inicio.get("totalPacientesAtivos").asLong());
        assertEquals(atencao, inicio.get("totalPacientesAtencao").asLong());
        long hoje = agendas.findAll().stream().filter(a -> a.getPaciente().getCapsReferencia() == CapsEnum.CAPS_AD
                && a.getDataAgendamento().equals(LocalDate.now()) && a.getSituacaoAtendimento() == SituacaoAtendimento.AGENDADO).count();
        long faltas = agendas.findAll().stream().filter(a -> a.getPaciente().getCapsReferencia() == CapsEnum.CAPS_AD
                && !a.getDataAgendamento().isBefore(LocalDate.now().minusDays(6)) && !a.getDataAgendamento().isAfter(LocalDate.now())
                && a.getSituacaoAtendimento() == SituacaoAtendimento.FALTOU).count();
        assertEquals(hoje, inicio.get("totalAgendamentosHoje").asLong());
        assertEquals(faltas, inicio.get("totalFaltasUltimosSeteDias").asLong());
        assertFalse(inicio.toString().contains(pI.getNome()));
    }

    @Test
    void filtrosDePainelNuncaSubstituemAutorizacao() throws Exception {
        var painel = ok(ad, "/api/painel/resumo");
        assertEquals(1, painel.get("unidadesDisponiveis").size());
        assertEquals("CAPS:CAPS_AD", painel.get("unidadesDisponiveis").get(0).get("valor").asString());
        var outroCaps = ok(ad, "/api/painel/resumo?unidade=CAPS:CAPS_I");
        assertEquals(0, outroCaps.get("totalPacientesAtivos").asInt());
        var outraUsf = ok(usfA, "/api/painel/resumo?unidade=USF:" + usfB.getUsfAtuacao().getCnes());
        assertEquals(0, outraUsf.get("totalPacientesAtivos").asInt());
        assertEquals(0, ok(ad, "/api/pacientes/pesquisa?q=" + pI.getCpf() + "&unidade=CAPS_I").get("totalRegistros").asInt());
    }

    @Test
    void agendaRejeitaProfissionalExternoEFiltraPacientes() throws Exception {
        String url = "/api/agendamentos/agenda/" + LocalDate.now();
        var r = getAs(ad, url);
        assertEquals(200, r.getStatus());
        assertTrue(r.getContentAsString().contains(pAd.getNome()));
        assertFalse(r.getContentAsString().contains(pI.getNome()));
        assertEquals(404, getAs(ad, url + "?profissionalId=" + coordenadorI.getIdPublico()).getStatus());
        assertEquals(404, getAs(ad, "/api/agendamentos/" + agendaI.getId()).getStatus());
        assertEquals(404, getAs(ad, "/api/agendamentos/" + agendaI.getId() + "/remarcacao").getStatus());
        assertEquals(404, getAs(ad, "/api/agendamentos/vagas?usuarioId=" + coordenadorI.getIdPublico() + "&data=" + LocalDate.now()).getStatus());
        assertEquals(404, getAs(ad, "/api/disponibilidade-config/disponibilidade?usuarioId=" + coordenadorI.getIdPublico()).getStatus());
        var selecao = ok(ad, "/api/usuarios/profissionais-selecao");
        assertFalse(selecao.toString().contains(coordenadorI.getIdPublico().toString()));
        assertTrue(selecao.toString().contains(coordenadorAd.getIdPublico().toString()));
    }

    @Test
    void gruposRejeitamIdsExternosEFiltramParticipantesLegados() throws Exception {
        assertEquals(404, postAs(ad, "/api/grupos/sessoes/" + futura.getId() + "/participantes",
                "{\"pacienteId\":\"" + pI.getIdPublico() + "\"}").getStatus());
        assertEquals(404, getAs(ad, "/api/grupos/" + estrangeira.getGrupo().getId()).getStatus());
        assertEquals(404, postAs(ad, "/api/grupos/sessoes", "{\"grupoId\":" + estrangeira.getGrupo().getId()
                + ",\"dataSessao\":\"" + LocalDate.now().plusDays(10) + "\",\"horario\":\"09:00:00\",\"participantesIds\":[]}").getStatus());
        String url = "/api/grupos/sessoes?dataInicio=" + LocalDate.now().minusDays(3) + "&dataFim=" + LocalDate.now().plusDays(6);
        var lista = ok(ad, url);
        var atual = lista.valueStream().filter(s -> s.get("id").asLong() == realizada.getId()).findFirst().orElseThrow();
        assertEquals(1, atual.get("quantidadeParticipantes").asInt());
        assertEquals(1, atual.get("quantidadePresencasConfirmadas").asInt());
        assertFalse(lista.toString().contains(pI.getNome()));
        var membros = ok(ad, "/api/grupos/" + futura.getGrupo().getId() + "/participantes");
        assertFalse(membros.toString().contains(pI.getNome()));
        assertTrue(membros.toString().contains(pAd.getNome()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "excel"})
    void exportacoesMantemSomenteDadosAutorizados(String formato) throws Exception {
        for (String tipo : List.of("busca-ativa", "frequencia-grupos")) {
            var response = getAs(ad, "/api/relatorios/" + tipo + "/" + formato);
            assertEquals(200, response.getStatus());
            String texto = extrair(response.getContentAsByteArray(), formato).replaceAll("\\s+", " ");
            if (tipo.equals("busca-ativa") || formato.equals("excel")) {
                assertTrue(texto.contains(pAd.getNome()), tipo + "/" + formato + ": paciente autorizado ausente");
            } else {
                // O PDF de frequência existente exibe grupos e contagens, sem nomes de pacientes.
                assertTrue(texto.contains(realizada.getGrupo().getTema()));
                assertFalse(texto.contains(estrangeira.getGrupo().getTema()));
            }
            assertFalse(texto.contains(pI.getNome()));
            assertFalse(texto.contains(pII.getNome()));
        }
        assertEquals(404, getAs(ad, "/api/relatorios/busca-ativa/" + formato
                + "?profissionalId=" + coordenadorI.getIdPublico()).getStatus());
        assertEquals(404, getAs(ad, "/api/relatorios/frequencia-grupos/" + formato
                + "?grupoId=" + estrangeira.getGrupo().getId()).getStatus());
    }

    private String extrair(byte[] bytes, String formato) throws Exception {
        if (formato.equals("pdf")) {
            try (PDDocument documento = PDDocument.load(bytes)) {
                return new PDFTextStripper().getText(documento);
            }
        }
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            StringBuilder texto = new StringBuilder();
            workbook.forEach(s -> s.forEach(r -> r.forEach(c -> texto.append(c.toString()).append('\n'))));
            return texto.toString();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/pacientes", "/api/pacientes/pesquisa", "/api/painel/resumo",
            "/api/inicio/resumo", "/api/grupos", "/api/usuarios/profissionais-selecao", "/api/relatorios/busca-ativa/excel"})
    void usfLegadaSemAssociacaoFalhaFechada(String url) throws Exception {
        var r = getAs(legado, url);
        assertEquals(422, r.getStatus(), url);
        assertTrue(r.getContentAsString().contains("A unidade USF específica deste usuário ainda não foi configurada."));
        assertFalse(r.getContentAsString().contains(pAd.getNome()));
        assertEquals(200, getAs(legado, "/api/usuarios/me").getStatus());
        // A administração geral permanece disponível para corrigir o usuário antigo.
        assertEquals(200, getAs(legado, "/api/usuarios").getStatus());
    }

    @Test
    void edicaoAdministrativaCorrigeUsfLegadaPelaApi() throws Exception {
        String body = json.writeValueAsString(java.util.Map.of(
                "nome", legado.getNome(), "email", legado.getEmail(), "tipoUsuario", "ADMINISTRADOR",
                "unidadeAtuacao", "USF", "usfAtuacaoId", usfA.getUsfAtuacao().getId()));
        var response = mvc.perform(put("/api/usuarios/" + legado.getIdPublico())
                .header("Authorization", "Bearer " + tokens.gerarToken(ad))
                .contentType("application/json").content(body)).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(usfA.getUsfAtuacao().getId(), json.readTree(response.getContentAsString()).get("usfAtuacaoId").asLong());
        assertEquals(200, getAs(legado, "/api/pacientes/pesquisa").getStatus());
        assertFalse(response.getContentAsString().contains("senha"));
    }

    @Test
    void agendamentoManualNaoAceitaPacienteDeOutraUnidade() throws Exception {
        String body = "{\"usuarioId\":\"" + coordenadorAd.getIdPublico() + "\",\"pacienteId\":\"" + pI.getIdPublico()
                + "\",\"dataAgendamento\":\"" + LocalDate.now().plusDays(2) + "\",\"turnoAgendamento\":\"MANHA\",\"horaAtendimento\":\"08:00:00\"}";
        assertEquals(404, postAs(ad, "/api/agendamentos", body).getStatus());
    }

    @Test
    void gruposNaoAlteramPacientesDeOutraUnidadeEmInscricaoOuConfirmacao() throws Exception {
        Long grupo = futura.getGrupo().getId();
        String paciente = "{\"pacienteId\":\"" + pI.getIdPublico() + "\"}";
        assertEquals(404, postAs(ad, "/api/grupos/" + grupo + "/inscricoes-futuras", paciente).getStatus());
        String retroativa = "{\"pacienteId\":\"" + pI.getIdPublico()
                + "\",\"frequenciasPassadas\":[{\"sessaoId\":" + realizada.getId() + ",\"statusPresenca\":\"PRESENTE\"}]}";
        assertEquals(404, postAs(ad, "/api/grupos/" + grupo + "/inscricoes-retroativas", retroativa).getStatus());
        participante(futura, pI, StatusPresencaGrupo.NAO_REGISTRADA);
        String cancelamento = "{\"ocorreu\":false,\"motivoCancelamento\":\"Teste de isolamento\"}";
        assertEquals(404, postAs(ad, "/api/grupos/sessoes/" + futura.getId() + "/confirmacao-ocorrencia", cancelamento).getStatus());
        assertEquals(StatusSessaoGrupo.AGENDADA, futura.getStatus());
        var remover = mvc.perform(delete("/api/grupos/sessoes/" + futura.getId() + "/participantes/" + pI.getIdPublico())
                .header("Authorization", "Bearer " + tokens.gerarToken(ad))).andReturn().getResponse();
        assertEquals(404, remover.getStatus());
        assertTrue(participantes.findBySessaoGrupoAndPaciente(futura, pI).isPresent());
    }

    @Test
    void filtrosPaginacaoEOrdenacaoExistentesContinuamDentroDoEscopo() throws Exception {
        var primeira = ok(ad, "/api/pacientes/pesquisa?q=Isolamento&size=1&page=0");
        var segunda = ok(ad, "/api/pacientes/pesquisa?q=Isolamento&size=1&page=1");
        assertEquals(2, primeira.get("totalRegistros").asInt());
        assertEquals(pAd.getNome(), primeira.get("pacientes").get(0).get("nome").asString());
        assertEquals(pAdB.getNome(), segunda.get("pacientes").get(0).get("nome").asString());
        pAdB.setClassificacaoRisco(ClassificacaoRisco.AMARELO);
        pAdB.setTipoAcompanhamento(TipoAcompanhamento.AMBOS);
        pacientes.saveAndFlush(pAdB);
        assertEquals(1, ok(ad, "/api/pacientes/pesquisa?q=Isolamento&classificacao=VERMELHO").get("totalRegistros").asInt());
        assertEquals(1, ok(ad, "/api/pacientes/pesquisa?q=Isolamento&tipoAcompanhamento=AMBOS").get("totalRegistros").asInt());
    }

    @Test
    void validacaoDeCpfDuplicadoContinuaAntesDaAtualizacao() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(ad, null, ad.getAuthorities()));
        try {
            var dados = json.convertValue(new PacienteDTO(pAd), java.util.Map.class);
            dados.put("countFaltas", null);
            dados.put("cpf", pAdB.getCpf());
            PacienteDTO dto = json.convertValue(dados, PacienteDTO.class);
            assertThrows(RecursoDuplicadoException.class, () -> pacienteService.save(dto, false));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

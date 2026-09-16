package com.pet.buscaativa.services.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.SessaoGrupo;
import com.pet.buscaativa.entities.dto.BuscaAtivaRelatorioDTO;
import com.pet.buscaativa.entities.dto.FrequenciaGrupoRelatorioDTO;
import com.pet.buscaativa.entities.enums.ClassificacaoRisco;
import com.pet.buscaativa.entities.enums.StatusPaciente;
import com.pet.buscaativa.entities.enums.StatusPresencaGrupo;
import com.pet.buscaativa.entities.enums.StatusSessaoGrupo;
import com.pet.buscaativa.entities.enums.TipoAcompanhamento;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.repositories.SessaoGrupoRepository;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.RelatorioService;
import com.pet.buscaativa.services.exceptions.RelatorioException;
import com.pet.buscaativa.services.exceptions.ValidationException;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;

@Service
@RequiredArgsConstructor
public class RelatorioServiceImpl implements RelatorioService {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String NAO_INFORMADO = "Não informado";

    private final PacienteRepository pacienteRepository;
    private final SessaoGrupoRepository sessaoGrupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final Map<String, JasperReport> templates = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public byte[] gerarBuscaAtiva(LocalDate dataInicio, LocalDate dataFim, UUID profissionalId,
                                  TipoAcompanhamento tipoAcompanhamento) {
        validarPeriodo(dataInicio, dataFim);
        List<BuscaAtivaRelatorioDTO> dados = pacienteRepository.findParaRelatorioBuscaAtiva(
                        StatusPaciente.ATIVO, ClassificacaoRisco.VERMELHO, dataInicio, dataFim,
                        profissionalId, tipoAcompanhamento)
                .stream().map(this::paraBuscaAtiva).toList();

        Map<String, Object> parametros = parametrosComuns(dataInicio, dataFim);
        parametros.put("PROFISSIONAL", profissionalId == null ? "Todos os profissionais" : usuarioRepository
                .findByIdPublico(profissionalId).map(u -> u.getNome()).orElse(NAO_INFORMADO));
        parametros.put("TIPO_ACOMPANHAMENTO", descricaoFiltro(tipoAcompanhamento));
        parametros.put("TOTAL", dados.size());
        return gerarPdf("relatorio-busca-ativa.jrxml", parametros, dados);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] gerarFrequenciaGrupos(LocalDate dataInicio, LocalDate dataFim, Long grupoId) {
        LocalDate fim = dataFim == null ? LocalDate.now(clock) : dataFim;
        LocalDate inicio = dataInicio == null ? fim.minusMonths(6) : dataInicio;
        validarPeriodo(inicio, fim);

        List<SessaoGrupo> sessoes = sessaoGrupoRepository
                .findRealizadasParaRelatorio(inicio, fim, StatusSessaoGrupo.REALIZADA, grupoId);

        List<FrequenciaGrupoRelatorioDTO> dados = sessoes.stream()
                .map(this::paraFrequencia)
                .toList();

        int presentes = sessoes.stream()
                .mapToInt(sessao -> contarPresencas(sessao, StatusPresencaGrupo.PRESENTE))
                .sum();
        int ausentes = sessoes.stream()
                .mapToInt(sessao -> contarPresencas(sessao, StatusPresencaGrupo.FALTOU))
                .sum();

        BigDecimal media = calcularTaxa(presentes, ausentes);
        Map<String, Object> parametros = parametrosComuns(inicio, fim);
        parametros.put("SESSOES", dados.size());
        parametros.put("PRESENCA_MEDIA", percentual(media));
        parametros.put("GRUPOS", dados.stream().map(FrequenciaGrupoRelatorioDTO::getGrupo).distinct().count());
        return gerarPdf("relatorio-frequencia-grupos.jrxml", parametros, dados);
    }

    BuscaAtivaRelatorioDTO paraBuscaAtiva(Paciente paciente) {
        String documento = texto(paciente.getCns());
        if (NAO_INFORMADO.equals(documento)) documento = texto(paciente.getCpf());
        String profissional = paciente.getProfissionalReferencia() == null
                ? NAO_INFORMADO : texto(paciente.getProfissionalReferencia().getNome());
        return new BuscaAtivaRelatorioDTO(texto(paciente.getNome()), documento,
                paciente.getDataUltimaPresenca(), paciente.getCountFaltas(),
                descricaoTipo(paciente.getTipoAcompanhamento()), profissional, texto(paciente.getTelefone()));
    }

    FrequenciaGrupoRelatorioDTO paraFrequencia(SessaoGrupo sessao) {
        List<String> presentes = nomes(sessao, StatusPresencaGrupo.PRESENTE);
        List<String> ausentes = nomes(sessao, StatusPresencaGrupo.FALTOU);
        BigDecimal taxa = calcularTaxa(presentes.size(), ausentes.size());
        int blocos = taxa.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).intValue();
        String indicador = "█".repeat(Math.min(10, blocos)) + "░".repeat(Math.max(0, 10 - blocos));
        return new FrequenciaGrupoRelatorioDTO(sessao.getDataSessao(), texto(sessao.getGrupo().getTema()),
                texto(sessao.getGrupo().getCoordenador().getNome()), presentes.size(), ausentes.size(),
                presentes, ausentes, taxa, percentual(taxa), indicador);
    }

    private int contarPresencas(SessaoGrupo sessao, StatusPresencaGrupo status) {
        return (int) sessao.getParticipantes().stream()
                .filter(participante -> participante.getStatusPresenca() == status)
                .count();
    }

    private List<String> nomes(SessaoGrupo sessao, StatusPresencaGrupo status) {
        return sessao.getParticipantes().stream().filter(p -> p.getStatusPresenca() == status)
                .map(p -> texto(p.getPaciente().getNome())).sorted().toList();
    }

    private BigDecimal calcularTaxa(int presentes, int ausentes) {
        int total = presentes + ausentes;
        return total == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(presentes)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private String percentual(BigDecimal valor) {
        return String.format(PT_BR, "%.1f%%", valor);
    }

    private Map<String, Object> parametrosComuns(LocalDate inicio, LocalDate fim) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("PERIODO", periodo(inicio, fim));
        parametros.put("GERADO_EM", LocalDateTime.now(clock).format(DATA_HORA));
        parametros.put("LOGO", getClass().getResourceAsStream("/reports/images/logo-sibraps.png"));
        return parametros;
    }

    private byte[] gerarPdf(String template, Map<String, Object> parametros, List<?> dados) {
        try {
            JasperReport report = templates.computeIfAbsent(template, this::compilar);
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(
                    report, parametros, new JRBeanCollectionDataSource(dados)));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RelatorioException("Não foi possível gerar o relatório em PDF.", e);
        }
    }

    private JasperReport compilar(String template) {
        try (InputStream input = new ClassPathResource("reports/" + template).getInputStream()) {
            return JasperCompileManager.compileReport(input);
        } catch (Exception e) {
            throw new RelatorioException("Não foi possível carregar o modelo do relatório.", e);
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new ValidationException("A data inicial deve ser anterior ou igual à data final.");
        }
    }

    private String periodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) return "Todo o período";
        return (inicio == null ? "Início" : inicio.format(DATA)) + " a " +
                (fim == null ? "Hoje" : fim.format(DATA));
    }

    private String descricaoFiltro(TipoAcompanhamento tipo) {
        return tipo == null ? "Individual e grupo" : descricaoTipo(tipo);
    }

    private String descricaoTipo(TipoAcompanhamento tipo) {
        if (tipo == null) return NAO_INFORMADO;
        return switch (tipo) {
            case INDIVIDUAL -> "Individual";
            case GRUPO_TERAPEUTICO -> "Grupo terapêutico";
            case AMBOS -> "Individual e grupo";
        };
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? NAO_INFORMADO : valor.trim();
    }
}

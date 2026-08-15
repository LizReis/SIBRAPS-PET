package com.pet.buscaativa.services.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.buscaativa.entities.*;
import com.pet.buscaativa.entities.dto.*;
import com.pet.buscaativa.entities.enums.*;
import com.pet.buscaativa.repositories.*;
import com.pet.buscaativa.services.GrupoTerapeuticoService;
import com.pet.buscaativa.services.exceptions.*;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GrupoTerapeuticoServiceImpl implements GrupoTerapeuticoService {

    private final GrupoTerapeuticoRepository grupoRepository;
    private final SessaoGrupoRepository sessaoRepository;
    private final SessaoGrupoParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final Clock clock;

    @Override
    @Transactional
    public GrupoTerapeuticoDTO criarGrupo(CriarGrupoDTO dto) {
        validarPeriodoRecorrencia(dto);
        Usuario coordenador = usuarioRepository.findByIdPublico(dto.coordenadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Coordenador não encontrado."));

        if (coordenador.getTipoUsuario() != TipoUsuario.PROFISSIONAL
                && coordenador.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
            throw new ValidationException("O coordenador do grupo deve ser um profissional ou administrador.");
        }

        if (dto.dataPrimeiraSessao().isBefore(LocalDate.now(clock))) {
            throw new ValidationException("Não é permitido criar um grupo com a primeira sessão em data passada.");
        }
        validarIdsParticipantes(dto.participantesIds());

        GrupoTerapeutico grupo = new GrupoTerapeutico();
        grupo.setTema(dto.tema());
        grupo.setCoordenador(coordenador);
        grupo.setRecorrencia(dto.recorrencia());
        grupo.setHorarioPadrao(dto.horario());
        grupo.setDataFimRecorrencia(dto.dataFimRecorrencia());
        grupo.setAtivo(true);

        grupo = grupoRepository.save(grupo);

        for (LocalDate data : gerarDatasDaSerie(dto.dataPrimeiraSessao(), dto.dataFimRecorrencia(), dto.recorrencia())) {
            SessaoGrupo sessao = novaSessao(grupo, data, dto.horario());
            sessao = sessaoRepository.save(sessao);
            for (UUID pacienteId : idsOuVazios(dto.participantesIds())) {
                adicionarParticipanteInterno(sessao, pacienteId);
            }
        }
        return toGrupoDTO(grupo);
    }

    /** Gera uma série finita, inclusiva; meses são sempre calculados a partir da data original. */
    List<LocalDate> gerarDatasDaSerie(LocalDate primeira, LocalDate fim, RecorrenciaGrupo recorrencia) {
        if (recorrencia == RecorrenciaGrupo.UNICA) return List.of(primeira);
        List<LocalDate> datas = new ArrayList<>();
        for (int ocorrencia = 0; ; ocorrencia++) {
            LocalDate data = switch (recorrencia) {
                case SEMANAL -> primeira.plusWeeks(ocorrencia);
                case QUINZENAL -> primeira.plusWeeks(2L * ocorrencia);
                case MENSAL -> primeira.plusMonths(ocorrencia);
                case UNICA -> primeira;
            };
            if (data.isAfter(fim)) break;
            datas.add(data);
        }
        return datas;
    }

    private void validarPeriodoRecorrencia(CriarGrupoDTO dto) {
        if (dto.recorrencia() != RecorrenciaGrupo.UNICA && dto.dataFimRecorrencia() == null) {
            throw new ValidationException("A data final da recorrência é obrigatória para grupos recorrentes.");
        }
        if (dto.dataFimRecorrencia() != null && dto.dataFimRecorrencia().isBefore(dto.dataPrimeiraSessao())) {
                throw new ValidationException("A data final da recorrência não pode ser anterior à primeira sessão.");
        }
    }

    private void validarIdsParticipantes(List<UUID> ids) {
        if (ids != null && new HashSet<>(ids).size() != ids.size()) {
            throw new ConflictException("A lista inicial contém paciente duplicado.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrupoTerapeuticoDTO> listarGrupos() {
        return grupoRepository.findByAtivoTrue().stream().map(this::toGrupoDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate sugerirProximaData(Long grupoId) {
        GrupoTerapeutico grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado."));

        LocalDate base = sessaoRepository.findFirstByGrupoOrderByDataSessaoDesc(grupo)
                .map(SessaoGrupo::getDataSessao).orElse(LocalDate.now(clock));

        return switch (grupo.getRecorrencia()) {
            case SEMANAL -> base.plusWeeks(1); case QUINZENAL -> base.plusWeeks(2);
            case MENSAL -> base.plusMonths(1); case UNICA -> base;
        };
    }

    @Override
    @Transactional
    public SessaoGrupoDTO criarProximaSessao(NovaSessaoDTO dto) {
        GrupoTerapeutico grupo = grupoRepository.findById(dto.grupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado."));

        if (!grupo.isAtivo()) throw new ValidationException("Não é possível criar sessões para um grupo inativo.");
        if (dto.dataSessao().isBefore(LocalDate.now(clock))) throw new ValidationException("Não é permitido criar sessão em data passada.");
        boolean existe = sessaoRepository.findByGrupoOrderByDataSessaoDesc(grupo).stream()
                .anyMatch(s -> s.getDataSessao().equals(dto.dataSessao()) && s.getStatus() != StatusSessaoGrupo.CANCELADA);
        if (existe) throw new ConflictException("Já existe uma sessão agendada para este grupo nesta data.");
        validarIdsParticipantes(dto.participantesIds());
        SessaoGrupo sessao = sessaoRepository.save(novaSessao(grupo, dto.dataSessao(),
                dto.horario() == null ? grupo.getHorarioPadrao() : dto.horario()));
        for (UUID id : idsOuVazios(dto.participantesIds())) adicionarParticipanteInterno(sessao, id);

        return toSessaoDTO(sessao);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessaoGrupoDTO> listarSessoes(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) throw new ValidationException("A data inicial não pode ser posterior à data final.");
        return sessaoRepository.findByDataSessaoBetween(inicio, fim).stream().map(this::toSessaoDTO).toList();
    }

    @Override
    @Transactional
    public SessaoGrupoDTO adicionarParticipante(Long sessaoId, AdicionarParticipanteDTO dto) {
        SessaoGrupo sessao = buscarSessao(sessaoId);

        validarSessaoEditavel(sessao);

        adicionarParticipanteInterno(sessao, dto.pacienteId());

        return toSessaoDTO(sessao);
    }

    @Override
    @Transactional
    public SessaoGrupoDTO removerParticipante(Long sessaoId, UUID pacienteId) {
        SessaoGrupo sessao = buscarSessao(sessaoId);

        validarSessaoEditavel(sessao);

        SessaoGrupoParticipante participante = buscarParticipante(sessao, pacienteId);

        sessao.getParticipantes().remove(participante);
        participanteRepository.delete(participante);

        return toSessaoDTO(sessao);
    }

    @Override
    @Transactional
    public SessaoGrupoDTO registrarPresenca(Long sessaoId, UUID pacienteId, RegistrarPresencaGrupoDTO dto) {
        SessaoGrupo sessao = buscarSessao(sessaoId);
        if (sessao.getStatus() == StatusSessaoGrupo.CANCELADA)
            throw new ValidationException("Não é possível registrar presença em sessão cancelada.");
        if (sessaoAindaNaoIniciou(sessao))
            throw new ValidationException("Não é possível registrar presença antes do início da sessão.");
        SessaoGrupoParticipante participante = buscarParticipante(sessao, pacienteId);
        participante.setStatusPresenca(dto.statusPresenca());
        participanteRepository.save(participante);
        return toSessaoDTO(sessao);
    }


    @Override
    @Transactional
    public SessaoGrupoDTO atualizarStatus(Long sessaoId, StatusSessaoGrupo novoStatus, Integer expectedVersion) {
        SessaoGrupo sessao = buscarSessao(sessaoId);
        if (expectedVersion != null && !expectedVersion.equals(sessao.getVersion()))
            throw new OptimisticLockException("A sessão foi alterada por outro usuário. Atualize a tela antes de tentar novamente.");
        if (sessao.getStatus() != novoStatus && (sessao.getStatus() != StatusSessaoGrupo.AGENDADA
                || (novoStatus != StatusSessaoGrupo.REALIZADA && novoStatus != StatusSessaoGrupo.CANCELADA))) {
            throw new ValidationException("Transição de status não permitida: " + sessao.getStatus() + " para " + novoStatus + ".");
        }

        sessao.setStatus(novoStatus);
        return toSessaoDTO(sessaoRepository.save(sessao));
    }

    private SessaoGrupo novaSessao(GrupoTerapeutico grupo, LocalDate data, LocalTime horario) {
        SessaoGrupo s = new SessaoGrupo(); s.setGrupo(grupo); s.setDataSessao(data);
        s.setHorario(horario); s.setStatus(StatusSessaoGrupo.AGENDADA); return s;
    }


    private void adicionarParticipanteInterno(SessaoGrupo sessao, UUID pacienteId) {
        Paciente paciente = pacienteRepository.findByIdPublico(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));

        if (paciente.getStatusPaciente() != StatusPaciente.ATIVO){
            throw new ValidationException("Somente pacientes ativos podem participar de grupos.");
        }

        if (participanteRepository.findBySessaoGrupoAndPaciente(sessao, paciente).isPresent())
            throw new ConflictException("Este paciente já está na lista de participantes desta sessão.");
        if (sessaoRepository.existsParticipacaoNoMesmoHorario(paciente, sessao.getDataSessao(), sessao.getHorario(), StatusSessaoGrupo.CANCELADA))
            throw new ConflictException("Conflito para o paciente " + paciente.getNome() + " em "
                    + sessao.getDataSessao() + " às " + sessao.getHorario() + ".");
        SessaoGrupoParticipante p = new SessaoGrupoParticipante(); p.setSessaoGrupo(sessao);
        p.setPaciente(paciente); p.setStatusPresenca(StatusPresencaGrupo.NAO_REGISTRADA);
        participanteRepository.save(p); sessao.getParticipantes().add(p);
    }


    private void validarSessaoEditavel(SessaoGrupo sessao) {
        if (sessao.getStatus() != StatusSessaoGrupo.AGENDADA)
            throw new ValidationException("Não é possível alterar participantes de uma sessão já realizada ou cancelada.");
        if (!sessaoAindaNaoIniciou(sessao))
            throw new ValidationException("Não é possível alterar participantes após o início da sessão.");
    }

    boolean sessaoAindaNaoIniciou(SessaoGrupo sessao) {
        return LocalDateTime.of(sessao.getDataSessao(), sessao.getHorario()).isAfter(LocalDateTime.now(clock));
    }

    private SessaoGrupo buscarSessao(Long id) { return sessaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada.")); }
    private SessaoGrupoParticipante buscarParticipante(SessaoGrupo s, UUID id) {
        Paciente p = pacienteRepository.findByIdPublico(id).orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));
        return participanteRepository.findBySessaoGrupoAndPaciente(s, p)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não é participante desta sessão."));
    }

    private List<UUID> idsOuVazios(List<UUID> ids) { 
        return ids == null ? List.of() : ids; 
    }

    private GrupoTerapeuticoDTO toGrupoDTO(GrupoTerapeutico g) {
        return new GrupoTerapeuticoDTO(g.getId(), g.getTema(), g.getCoordenador().getIdPublico(),
                g.getCoordenador().getNome(), g.getRecorrencia(), g.getHorarioPadrao(),
                g.getDataFimRecorrencia(), g.isAtivo());
    }

    private SessaoGrupoDTO toSessaoDTO(SessaoGrupo s) {
        List<ParticipanteSessaoDTO> ps = s.getParticipantes().stream()
                .map(p -> new ParticipanteSessaoDTO(p.getPaciente().getIdPublico(), p.getPaciente().getNome(), p.getStatusPresenca())).toList();
        int presencas = (int) s.getParticipantes().stream().filter(p -> p.getStatusPresenca() == StatusPresencaGrupo.PRESENTE).count();
        return new SessaoGrupoDTO(s.getId(), s.getGrupo().getId(), s.getGrupo().getTema(),
                s.getGrupo().getCoordenador().getNome(), s.getDataSessao(), s.getHorario(), s.getStatus(),
                statusExibicao(s), ps, ps.size(), presencas, s.getVersion());
    }

    private StatusExibicaoSessaoGrupo statusExibicao(SessaoGrupo s) {
        return switch (s.getStatus()) {
            case REALIZADA -> StatusExibicaoSessaoGrupo.REALIZADO;
            case CANCELADA -> StatusExibicaoSessaoGrupo.CANCELADO;
            case AGENDADA -> sessaoAindaNaoIniciou(s) ? StatusExibicaoSessaoGrupo.AGENDADO : StatusExibicaoSessaoGrupo.EM_ANDAMENTO;
        };
    }
}
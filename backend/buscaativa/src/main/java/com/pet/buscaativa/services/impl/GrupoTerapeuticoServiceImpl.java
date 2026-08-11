package com.pet.buscaativa.services.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.GrupoTerapeutico;
import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.SessaoGrupo;
import com.pet.buscaativa.entities.SessaoGrupoParticipante;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.AdicionarParticipanteDTO;
import com.pet.buscaativa.entities.dto.CriarGrupoDTO;
import com.pet.buscaativa.entities.dto.GrupoTerapeuticoDTO;
import com.pet.buscaativa.entities.dto.NovaSessaoDTO;
import com.pet.buscaativa.entities.dto.ParticipanteSessaoDTO;
import com.pet.buscaativa.entities.dto.SessaoGrupoDTO;
import com.pet.buscaativa.entities.enums.StatusPaciente;
import com.pet.buscaativa.entities.enums.StatusSessaoGrupo;
import com.pet.buscaativa.entities.enums.TipoUsuario;
import com.pet.buscaativa.repositories.GrupoTerapeuticoRepository;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.repositories.SessaoGrupoParticipanteRepository;
import com.pet.buscaativa.repositories.SessaoGrupoRepository;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.GrupoTerapeuticoService;
import com.pet.buscaativa.services.exceptions.ConflictException;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;
import com.pet.buscaativa.services.exceptions.ValidationException;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GrupoTerapeuticoServiceImpl implements GrupoTerapeuticoService {

    private final GrupoTerapeuticoRepository grupoRepository;
    private final SessaoGrupoRepository sessaoRepository;
    private final SessaoGrupoParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    @Override
    @Transactional
    public GrupoTerapeuticoDTO criarGrupo(CriarGrupoDTO dto) {
        Usuario coordenador = usuarioRepository.findByIdPublico(dto.coordenadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Coordenador não encontrado."));

        if (coordenador.getTipoUsuario() != TipoUsuario.PROFISSIONAL
                && coordenador.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
            throw new ValidationException("O coordenador do grupo deve ser um profissional ou administrador.");
        }

        if (dto.dataPrimeiraSessao().isBefore(LocalDate.now())) {
            throw new ValidationException("Não é permitido criar um grupo com a primeira sessão em data passada.");
        }

        GrupoTerapeutico grupo = new GrupoTerapeutico();
        grupo.setTema(dto.tema());
        grupo.setCoordenador(coordenador);
        grupo.setRecorrencia(dto.recorrencia());
        grupo.setHorarioPadrao(dto.horario());
        grupo.setAtivo(true);

        grupo = grupoRepository.save(grupo);

        SessaoGrupo primeiraSessao = new SessaoGrupo();
        primeiraSessao.setGrupo(grupo);
        primeiraSessao.setDataSessao(dto.dataPrimeiraSessao());
        primeiraSessao.setHorario(dto.horario());
        primeiraSessao.setStatus(StatusSessaoGrupo.AGENDADA);

        primeiraSessao = sessaoRepository.save(primeiraSessao);

        if (dto.participantesIds() != null) {
            for (UUID pacienteId : dto.participantesIds()) {
                adicionarParticipanteInterno(primeiraSessao, pacienteId);
            }
        }

        return toGrupoDTO(grupo);
    }

    @Override
    public List<GrupoTerapeuticoDTO> listarGrupos() {
        return grupoRepository.findByAtivoTrue().stream()
                .map(this::toGrupoDTO)
                .toList();
    }

    @Override
    public LocalDate sugerirProximaData(Long grupoId) {
        GrupoTerapeutico grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado."));

        LocalDate baseData = sessaoRepository.findFirstByGrupoOrderByDataSessaoDesc(grupo)
                .map(SessaoGrupo::getDataSessao)
                .orElse(LocalDate.now());

        return switch (grupo.getRecorrencia()) {
            case SEMANAL -> baseData.plusWeeks(1);
            case QUINZENAL -> baseData.plusWeeks(2);
            case MENSAL -> baseData.plusMonths(1);
            case UNICA -> baseData;
        };
    }

    @Override
    @Transactional
    public SessaoGrupoDTO criarProximaSessao(NovaSessaoDTO dto) {
        GrupoTerapeutico grupo = grupoRepository.findById(dto.grupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado."));

        if (!grupo.isAtivo()) {
            throw new ValidationException("Não é possível criar sessões para um grupo inativo.");
        }

        if (dto.dataSessao().isBefore(LocalDate.now())) {
            throw new ValidationException("Não é permitido criar sessão em data passada.");
        }

        boolean jaExisteNaData = sessaoRepository.findByGrupoOrderByDataSessaoDesc(grupo).stream()
                .anyMatch(s -> s.getDataSessao().equals(dto.dataSessao())
                        && s.getStatus() != StatusSessaoGrupo.CANCELADA);

        if (jaExisteNaData) {
            throw new ConflictException("Já existe uma sessão agendada para este grupo nesta data.");
        }

        LocalTime horario = dto.horario() != null ? dto.horario() : grupo.getHorarioPadrao();

        SessaoGrupo sessao = new SessaoGrupo();
        sessao.setGrupo(grupo);
        sessao.setDataSessao(dto.dataSessao());
        sessao.setHorario(horario);
        sessao.setStatus(StatusSessaoGrupo.AGENDADA);

        sessao = sessaoRepository.save(sessao);

        if (dto.participantesIds() != null) {
            for (UUID pacienteId : dto.participantesIds()) {
                adicionarParticipanteInterno(sessao, pacienteId);
            }
        }

        return toSessaoDTO(sessao);
    }

    @Override
    public List<SessaoGrupoDTO> listarSessoes(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio.isAfter(dataFim)) {
            throw new ValidationException("A data inicial não pode ser posterior à data final.");
        }

        return sessaoRepository.findByDataSessaoBetween(dataInicio, dataFim).stream()
                .map(this::toSessaoDTO)
                .toList();
    }

    @Override
    @Transactional
    public SessaoGrupoDTO adicionarParticipante(Long sessaoId, AdicionarParticipanteDTO dto) {
        SessaoGrupo sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada."));

        validarSessaoEditavel(sessao);

        adicionarParticipanteInterno(sessao, dto.pacienteId());

        return toSessaoDTO(sessao);
    }

    @Override
    @Transactional
    public SessaoGrupoDTO removerParticipante(Long sessaoId, UUID pacienteId) {
        SessaoGrupo sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada."));

        validarSessaoEditavel(sessao);

        Paciente paciente = pacienteRepository.findByIdPublico(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));

        SessaoGrupoParticipante participante = participanteRepository
                .findBySessaoGrupoAndPaciente(sessao, paciente)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não é participante desta sessão."));

        sessao.getParticipantes().remove(participante);
        participanteRepository.delete(participante);

        return toSessaoDTO(sessao);
    }

    @Override
    @Transactional
    public SessaoGrupoDTO atualizarStatus(Long sessaoId, StatusSessaoGrupo novoStatus, Integer expectedVersion) {
        SessaoGrupo sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada."));

        if (expectedVersion != null && !expectedVersion.equals(sessao.getVersion())) {
            throw new OptimisticLockException(
                    "A sessão foi alterada por outro usuário. Atualize a tela antes de tentar novamente.");
        }

        sessao.setStatus(novoStatus);
        sessao = sessaoRepository.save(sessao);

        return toSessaoDTO(sessao);
    }

    private void adicionarParticipanteInterno(SessaoGrupo sessao, UUID pacienteId) {
        Paciente paciente = pacienteRepository.findByIdPublico(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));

        if (paciente.getStatusPaciente() != StatusPaciente.ATIVO) {
            throw new ValidationException("Somente pacientes ativos podem participar de grupos.");
        }

        if (participanteRepository.findBySessaoGrupoAndPaciente(sessao, paciente).isPresent()) {
            throw new ConflictException("Este paciente já está na lista de participantes desta sessão.");
        }

        boolean conflitoHorario = sessaoRepository.existsParticipacaoNoMesmoHorario(
                paciente, sessao.getDataSessao(), sessao.getHorario(), StatusSessaoGrupo.CANCELADA);

        if (conflitoHorario) {
            throw new ConflictException(
                    "O paciente já participa de outro grupo na mesma data e horário.");
        }

        SessaoGrupoParticipante participante = new SessaoGrupoParticipante();
        participante.setSessaoGrupo(sessao);
        participante.setPaciente(paciente);

        participante = participanteRepository.save(participante);
        sessao.getParticipantes().add(participante);
    }

    private void validarSessaoEditavel(SessaoGrupo sessao) {
        if (sessao.getStatus() != StatusSessaoGrupo.AGENDADA) {
            throw new ValidationException(
                    "Não é possível alterar participantes de uma sessão já realizada ou cancelada.");
        }
        if (sessao.getDataSessao().isBefore(LocalDate.now())) {
            throw new ValidationException("Não é possível alterar participantes de uma sessão que já ocorreu.");
        }
    }

    private GrupoTerapeuticoDTO toGrupoDTO(GrupoTerapeutico g) {
        return new GrupoTerapeuticoDTO(
                g.getId(),
                g.getTema(),
                g.getCoordenador().getIdPublico(),
                g.getCoordenador().getNome(),
                g.getRecorrencia(),
                g.getHorarioPadrao(),
                g.isAtivo()
        );
    }

    private SessaoGrupoDTO toSessaoDTO(SessaoGrupo s) {
        List<ParticipanteSessaoDTO> participantes = s.getParticipantes().stream()
                .map(p -> new ParticipanteSessaoDTO(p.getPaciente().getIdPublico(), p.getPaciente().getNome()))
                .toList();

        return new SessaoGrupoDTO(
                s.getId(),
                s.getGrupo().getId(),
                s.getGrupo().getTema(),
                s.getGrupo().getCoordenador().getNome(),
                s.getDataSessao(),
                s.getHorario(),
                s.getStatus(),
                participantes,
                participantes.size(),
                s.getVersion()
        );
    }
}
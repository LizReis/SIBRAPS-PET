package com.pet.buscaativa.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.buscaativa.entities.Agendamento;
import com.pet.buscaativa.entities.HistoricoPaciente;
import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.HistoricoPacienteDTO;
import com.pet.buscaativa.entities.dto.HistoricoPacienteEventoDTO;
import com.pet.buscaativa.entities.dto.RegistroHistoricoPacienteDTO;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.TipoAcompanhamento;
import com.pet.buscaativa.entities.enums.TipoEventoHistoricoPaciente;
import com.pet.buscaativa.repositories.HistoricoPacienteRepository;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.HistoricoPacienteService;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;
import com.pet.buscaativa.services.exceptions.ValidationException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoricoPacienteServiceImpl implements HistoricoPacienteService {

    private final HistoricoPacienteRepository historicoRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public HistoricoPacienteDTO consultar(UUID pacienteId) {
        Paciente paciente = pacienteRepository.findByIdPublico(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException(pacienteId));

        List<HistoricoPacienteEventoDTO> eventos = historicoRepository
                .findByPacienteIdPublicoOrderByOcorridoEmDesc(pacienteId)
                .stream()
                .map(this::toEventoDTO)
                .toList();

        return new HistoricoPacienteDTO(paciente.getIdPublico(), paciente.getNome(),
                paciente.getStatusPaciente(), eventos);
    }

    @Override
    @Transactional
    public void registrarManual(UUID pacienteId, RegistroHistoricoPacienteDTO registro) {
        if (registro.tipo() != TipoEventoHistoricoPaciente.BUSCA_ATIVA
                && registro.tipo() != TipoEventoHistoricoPaciente.PARTICIPACAO_GRUPO_TERAPEUTICO) {
            throw new ValidationException("Somente registros de busca ativa ou participação em grupo podem ser incluídos manualmente.");
        }

        Paciente paciente = pacienteRepository.findByIdPublico(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException(pacienteId));
        salvar(paciente, null, usuarioLogado(), registro.tipo(), null,
                registro.ocorridoEm() == null ? LocalDateTime.now() : registro.ocorridoEm(), registro.descricao());
    }

    @Override
    @Transactional
    public void registrarAgendamento(Agendamento agendamento) {
        salvar(agendamento.getPaciente(), agendamento, agendamento.getUsuario(),
                TipoEventoHistoricoPaciente.CONSULTA_AGENDADA, agendamento.getSituacaoAtendimento(),
                dataDoAgendamento(agendamento), "Consulta agendada.");
    }

    @Override
    @Transactional
    public void registrarAlteracaoDeAtendimento(Agendamento agendamento, SituacaoAtendimento statusAnterior) {
        SituacaoAtendimento novoStatus = agendamento.getSituacaoAtendimento();
        TipoEventoHistoricoPaciente tipo = switch (novoStatus) {
            case PRESENTE -> agendamento.getPaciente().getTipoAcompanhamento() == TipoAcompanhamento.GRUPO_TERAPEUTICO
                    ? TipoEventoHistoricoPaciente.PARTICIPACAO_GRUPO_TERAPEUTICO
                    : TipoEventoHistoricoPaciente.PRESENCA;
            case FALTOU -> TipoEventoHistoricoPaciente.FALTA;
            case REMARCADO, REMARCADO_ORIGEM -> TipoEventoHistoricoPaciente.REMARCACAO;
            default -> null;
        };

        if (tipo != null) {
            salvar(agendamento.getPaciente(), agendamento, agendamento.getUsuario(), tipo, novoStatus,
                    LocalDateTime.now(), "Situação alterada de " + statusAnterior + " para " + novoStatus + ".");
        }
    }

    @Override
    @Transactional
    public void registrarSituacaoAtual(Paciente paciente, String descricao) {
        salvar(paciente, null, usuarioLogado(), TipoEventoHistoricoPaciente.SITUACAO_ATUALIZADA,
                null, LocalDateTime.now(), descricao);
    }

    private void salvar(Paciente paciente, Agendamento agendamento, Usuario profissional,
            TipoEventoHistoricoPaciente tipo, SituacaoAtendimento situacao, LocalDateTime ocorridoEm,
            String descricao) {
        HistoricoPaciente historico = new HistoricoPaciente();
        historico.setPaciente(paciente);
        historico.setAgendamento(agendamento);
        historico.setProfissional(profissional);
        historico.setTipo(tipo);
        historico.setSituacaoAtendimento(situacao);
        historico.setOcorridoEm(ocorridoEm);
        historico.setDescricao(descricao);
        historicoRepository.save(historico);
    }

    private LocalDateTime dataDoAgendamento(Agendamento agendamento) {
        if (agendamento.getDataAgendamento() != null && agendamento.getHoraAtendimento() != null) {
            return LocalDateTime.of(agendamento.getDataAgendamento(), agendamento.getHoraAtendimento());
        }
        return LocalDateTime.now();
    }

    private Usuario usuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
        return email == null ? null : usuarioRepository.findByEmail(email).orElse(null);
    }

    private HistoricoPacienteEventoDTO toEventoDTO(HistoricoPaciente evento) {
        return new HistoricoPacienteEventoDTO(evento.getId(), evento.getTipo(), evento.getSituacaoAtendimento(),
                evento.getOcorridoEm(), evento.getDescricao(),
                evento.getAgendamento() == null ? null : evento.getAgendamento().getId(),
                evento.getProfissional() == null ? evento.getCreatedBy() : evento.getProfissional().getNome());
    }
}
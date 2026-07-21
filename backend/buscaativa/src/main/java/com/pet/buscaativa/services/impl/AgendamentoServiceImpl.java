package com.pet.buscaativa.services.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Agendamento;
import com.pet.buscaativa.entities.BloqueioAgenda;
import com.pet.buscaativa.entities.Disponibilidade;
import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.AgendamentoDTO;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.TipoAcompanhamento;
import com.pet.buscaativa.entities.enums.TipoUsuario;
import com.pet.buscaativa.entities.enums.TurnoEnum;
import com.pet.buscaativa.mapping.AgendamentoMapper;
import com.pet.buscaativa.repositories.AgendamentoRepository;
import com.pet.buscaativa.repositories.BloqueioAgendaRepository;
import com.pet.buscaativa.repositories.DisponibilidadeRepository;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.AgendamentoService;
import com.pet.buscaativa.services.PacienteService;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;
import com.pet.buscaativa.services.exceptions.ValidationException;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgendamentoServiceImpl implements AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final PacienteRepository pacienteRepository;

    private final AgendamentoMapper agendamentoMapper;

    private final PacienteService pacienteService;

    @Override
    public AgendamentoDTO save(AgendamentoDTO agendamentoDTO) {
        Usuario usuario = usuarioRepository.findByIdPublico(agendamentoDTO.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Paciente paciente = pacienteRepository.findByIdPublico(agendamentoDTO.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        Agendamento agendamento = new Agendamento();
        agendamento.setUsuario(usuario);
        agendamento.setPaciente(paciente);
        agendamento.setDataAgendamento(agendamentoDTO.dataAgendamento());
        agendamento.setTurnoAgendamento(agendamentoDTO.turnoAgendamento());
        agendamento.setHoraAtendimento(agendamentoDTO.horaAtendimento());

        if (agendamentoDTO.id() != null) {
            var original = agendamentoRepository.findById(agendamentoDTO.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Agendamento original não encontrado"));

            if (paciente.getTipoAcompanhamento() == TipoAcompanhamento.GRUPO_TERAPEUTICO) {
                throw new ValidationException(
                        "Remarcação individual não permitida para atendimentos de Grupo Terapêutico.");
            }

            original.setSituacaoAtendimento(SituacaoAtendimento.REMARCADO_ORIGEM);
            agendamento.setAgendamentoOriginal(original);
            agendamento.setSituacaoAtendimento(SituacaoAtendimento.REMARCADO);
        } else {
            agendamento.setSituacaoAtendimento(SituacaoAtendimento.AGENDADO);
        }

        agendamento = agendamentoRepository.save(agendamento);
        return agendamentoMapper.toAgendamentoDTO(agendamento);
    }

    @Override
    public List<LocalDate> sugerirDataRemarcacao(Long agendamento) {
        var original = agendamentoRepository.findById(agendamento)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (original.getPaciente() != null
                && original.getPaciente().getTipoAcompanhamento() == TipoAcompanhamento.GRUPO_TERAPEUTICO) {
            throw new ValidationException(
                    "Não é possível sugerir remarcação individual para atendimentos de Grupo Terapêutico.");
        }

        return buscarProximasVagasDisponiveis(original.getUsuario(), original.getTurnoAgendamento(), LocalDate.now(),
                3);
    }

    @Override
    public Map<TurnoEnum, Integer> calcularVagasDisponiveis(UUID usuarioIdPublico, LocalDate data) {
        Usuario usuario = usuarioRepository.findByIdPublico(usuarioIdPublico)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + usuarioIdPublico));

        Map<TurnoEnum, Integer> vagasPorTurno = new HashMap<>();
        vagasPorTurno.put(TurnoEnum.MANHA, 0);
        vagasPorTurno.put(TurnoEnum.TARDE, 0);

        boolean isBloqueado = bloqueioAgendaRepository.isDataBloqueadaParaUsuario(usuario, data);
        if (isBloqueado) {
            return vagasPorTurno;
        }

        DayOfWeek diaSemana = data.getDayOfWeek();

        List<SituacaoAtendimento> ocupantesVaga = List.of(
                SituacaoAtendimento.AGENDADO,
                SituacaoAtendimento.REMARCADO,
                SituacaoAtendimento.PRESENTE);

        Optional<Disponibilidade> manhaOpt = disponibilidadeRepository.findByUsuarioAndDiaDaSemanaAndTurno(usuario,
                diaSemana, TurnoEnum.MANHA);
        Optional<Disponibilidade> tardeOpt = disponibilidadeRepository.findByUsuarioAndDiaDaSemanaAndTurno(usuario,
                diaSemana, TurnoEnum.TARDE);

        if (manhaOpt.isPresent()) {
            Disponibilidade manha = manhaOpt.get();
            int ocupadas = agendamentoRepository.contarVagasOcupadasBySituacoes(usuario, data, TurnoEnum.MANHA,
                    ocupantesVaga);
            vagasPorTurno.put(TurnoEnum.MANHA, Math.max(0, manha.getCapacidade() - ocupadas));
        }

        if (tardeOpt.isPresent()) {
            Disponibilidade tarde = tardeOpt.get();
            int ocupadas = agendamentoRepository.contarVagasOcupadasBySituacoes(usuario, data, TurnoEnum.TARDE,
                    ocupantesVaga);
            vagasPorTurno.put(TurnoEnum.TARDE, Math.max(0, tarde.getCapacidade() - ocupadas));
        }

        return vagasPorTurno;
    }

    @Override
    public List<AgendamentoDTO> findAll() {
        var list = agendamentoRepository.findAll();
        return list.stream().map(agendamentoMapper::toAgendamentoDTO).toList();
    }

    @Override
    public List<LocalDate> buscarProximasVagasDisponiveis(Usuario usuario, TurnoEnum turno, LocalDate dataInicio,
                                                          int quantidadeDesejada) {
        List<LocalDate> datasDisponiveis = new ArrayList<>();

        int limiteDiasBusca = 90;
        LocalDate primeiraData = dataInicio.plusDays(1);
        LocalDate ultimaData = dataInicio.plusDays(limiteDiasBusca);

        List<Disponibilidade> todasDisponibilidades = disponibilidadeRepository.findByUsuario(usuario);
        Map<DayOfWeek, Map<TurnoEnum, Disponibilidade>> disponibilidadeMap = new HashMap<>();
        for (Disponibilidade d : todasDisponibilidades) {
            disponibilidadeMap
                    .computeIfAbsent(d.getDiaDaSemana(), k -> new HashMap<>())
                    .put(d.getTurno(), d);
        }

        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findByUsuario(usuario);
        Set<LocalDate> datasBloqueadas = new HashSet<>();
        for (BloqueioAgenda b : bloqueios) {
            LocalDate start = b.getDataInicio();
            LocalDate end = b.getDataFim();
            if (end == null && start == null)
                continue;

            LocalDate s = (start == null || start.isBefore(primeiraData)) ? primeiraData : start;
            LocalDate e = (end == null || end.isAfter(ultimaData)) ? ultimaData : end;

            if (s.isAfter(e))
                continue;

            LocalDate cursor = s;
            while (!cursor.isAfter(e)) {
                datasBloqueadas.add(cursor);
                cursor = cursor.plusDays(1);
            }
        }

        List<Agendamento> agendamentosNoIntervalo = agendamentoRepository
                .findByUsuarioAndDataAgendamentoBetween(usuario, primeiraData, ultimaData);
        List<SituacaoAtendimento> ocupantes = List.of(SituacaoAtendimento.AGENDADO, SituacaoAtendimento.REMARCADO,
                SituacaoAtendimento.PRESENTE);

        Map<LocalDate, Map<TurnoEnum, Integer>> ocupacaoMap = new HashMap<>();
        for (Agendamento a : agendamentosNoIntervalo) {
            if (a.getDataAgendamento() == null || a.getTurnoAgendamento() == null || a.getSituacaoAtendimento() == null)
                continue;

            if (!ocupantes.contains(a.getSituacaoAtendimento()))
                continue;

            LocalDate d = a.getDataAgendamento();
            TurnoEnum t = a.getTurnoAgendamento();

            ocupacaoMap
                    .computeIfAbsent(d, k -> new HashMap<>())
                    .merge(t, 1, Integer::sum);
        }

        LocalDate dataVerificacao = primeiraData;
        int diasBuscados = 0;

        while (datasDisponiveis.size() < quantidadeDesejada && diasBuscados < limiteDiasBusca) {
            diasBuscados++;

            if (dataVerificacao.isAfter(ultimaData))
                break;

            if (datasBloqueadas.contains(dataVerificacao)) {
                dataVerificacao = dataVerificacao.plusDays(1);
                continue;
            }

            DayOfWeek diaSemana = dataVerificacao.getDayOfWeek();
            Map<TurnoEnum, Disponibilidade> porTurno = disponibilidadeMap.get(diaSemana);
            if (porTurno == null) {
                dataVerificacao = dataVerificacao.plusDays(1);
                continue;
            }

            Disponibilidade disponibilidade = porTurno.get(turno);
            if (disponibilidade == null) {
                dataVerificacao = dataVerificacao.plusDays(1);
                continue;
            }

            int ocupadas = 0;
            Map<TurnoEnum, Integer> porTurnoCount = ocupacaoMap.get(dataVerificacao);
            if (porTurnoCount != null && porTurnoCount.get(turno) != null) {
                ocupadas = porTurnoCount.get(turno);
            }

            if (ocupadas < disponibilidade.getCapacidade()) {
                datasDisponiveis.add(dataVerificacao);
            }

            dataVerificacao = dataVerificacao.plusDays(1);
        }

        return datasDisponiveis;
    }

    @Override
    public List<AgendamentoDTO> buscarAgendaDoDia(LocalDate data, String emailLogado, UUID profissionalIdPublico) {
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário logado não encontrado"));

        List<Agendamento> agendamentos;

        if (usuarioLogado.getTipoUsuario() == TipoUsuario.PROFISSIONAL) {
            agendamentos = agendamentoRepository.findByDataAgendamentoAndUsuario(data, usuarioLogado);
        } else {
            if (profissionalIdPublico != null) {
                Usuario profissionalAlvo = usuarioRepository.findByIdPublico(profissionalIdPublico)
                        .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado"));

                agendamentos = agendamentoRepository.findByDataAgendamentoAndUsuario(data, profissionalAlvo);
            } else {
                agendamentos = agendamentoRepository.findByDataAgendamento(data);
            }
        }

        return agendamentos.stream()
                .map(agendamentoMapper::toAgendamentoDTO)
                .toList();
    }

    @Override
    @Transactional
    public AgendamentoDTO atualizarStatus(Long id, SituacaoAtendimento novoStatus, Integer expectedVersion) {
       Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado para o id"));

                LocalDate dataAgendamento = agendamento.getDataAgendamento();
        if (dataAgendamento != null && dataAgendamento.isAfter(LocalDate.now())) {
            throw new ValidationException(
                    "Não é permitido registrar presença ou falta para agendamentos com data futura.");
        }

        if (expectedVersion != null && !expectedVersion.equals(agendamento.getVersion())) {
            throw new OptimisticLockException(
                    "O agendamento foi alterado por outro usuário. Atualize a agenda antes de tentar novamente.");
        }

        SituacaoAtendimento statusAnterior = agendamento.getSituacaoAtendimento();
        
        if (statusAnterior == novoStatus) {
            return agendamentoMapper.toAgendamentoDTO(agendamento);
        }

        agendamento.setSituacaoAtendimento(novoStatus);

        Paciente pacienteAgendado = agendamento.getPaciente();
        if (pacienteAgendado != null) {
            Paciente paciente = pacienteRepository.findByIdPublicoForUpdate(pacienteAgendado.getIdPublico())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

            pacienteService.atualizarAssiduidadePaciente(paciente, statusAnterior, novoStatus);

            // 6. RF08: Gatilho de Visita Domiciliar Automático
            // Se o status virou FALTOU e este agendamento é uma remarcação (tem um original vinculado):
            if (novoStatus == SituacaoAtendimento.FALTOU && agendamento.getAgendamentoOriginal() != null) {
                paciente.setGatilhoVisitaAcionado(true);
                pacienteRepository.save(paciente);
            }

            // O Paciente já será salvo automaticamente ao final do método por causa do @Transactional,
            // mas manter o save explícito não causa problemas se preferir.
        }

        agendamento = agendamentoRepository.save(agendamento);

        return agendamentoMapper.toAgendamentoDTO(agendamento);
    }

}
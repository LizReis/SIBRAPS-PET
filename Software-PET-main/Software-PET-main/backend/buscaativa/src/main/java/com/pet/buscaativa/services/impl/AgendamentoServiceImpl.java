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

    @Override
    public AgendamentoDTO save(AgendamentoDTO agendamentoDTO) {
        Usuario usuario = usuarioRepository.findById(agendamentoDTO.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Paciente paciente = pacienteRepository.findById(agendamentoDTO.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        Agendamento agendamento = new Agendamento();
        agendamento.setUsuario(usuario);
        agendamento.setPaciente(paciente);
        agendamento.setDataAgendamento(agendamentoDTO.dataAgendamento());
        agendamento.setTurnoAgendamento(agendamentoDTO.turnoAgendamento());
        agendamento.setHoraAtendimento(agendamentoDTO.horaAtendimento());

        if (agendamentoDTO.id() != null) {
            var original = agendamentoRepository.findById(agendamentoDTO.id())
                    .orElseThrow(() -> new RuntimeException("Agendamento original não encontrado"));

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
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        // se original for Grupo Terapêutico, não sugerir remarcação individual
        if (original.getPaciente() != null
                && original.getPaciente().getTipoAcompanhamento() == TipoAcompanhamento.GRUPO_TERAPEUTICO) {
            throw new ValidationException(
                    "Não é possível sugerir remarcação individual para atendimentos de Grupo Terapêutico.");
        }

        return buscarProximasVagasDisponiveis(original.getUsuario(), original.getTurnoAgendamento(), LocalDate.now(),
                3);
    }

    @Override
    public int calcularVagasDisponiveis(Long usuarioId, LocalDate data) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + usuarioId));

        // Se a data cai em uma data bloqueada, a disponibilidade é ZERO
        boolean isBloqueado = bloqueioAgendaRepository.isDataBloqueadaParaUsuario(usuario, data);
        if (isBloqueado) {
            return 0;
        }

        DayOfWeek diaSemana = data.getDayOfWeek();
        int vagasDisponiveisTotal = 0;

        // Define quais status consideramos como ocupantes de vaga
        List<SituacaoAtendimento> ocupantesVaga = List.of(
                SituacaoAtendimento.AGENDADO,
                SituacaoAtendimento.REMARCADO,
                SituacaoAtendimento.PRESENTE);

        Optional<Disponibilidade> manhaOpt = disponibilidadeRepository.findByUsuarioAndDiaDaSemanaAndTurno(usuario,
                diaSemana, TurnoEnum.MANHA);
        Optional<Disponibilidade> tardeOpt = disponibilidadeRepository.findByUsuarioAndDiaDaSemanaAndTurno(usuario,
                diaSemana, TurnoEnum.TARDE);

        // Busca a disponibilidade da Manhã e desconta os ocupados
        if (manhaOpt.isPresent()) {
            Disponibilidade manha = manhaOpt.get();
            int ocupadas = agendamentoRepository.contarVagasOcupadasBySituacoes(usuario, data, TurnoEnum.MANHA,
                    ocupantesVaga);
            vagasDisponiveisTotal += Math.max(0, manha.getCapacidade() - ocupadas);
        }

        // Busca a disponibilidade da Tarde e desconta os ocupados
        if (tardeOpt.isPresent()) {
            Disponibilidade tarde = tardeOpt.get();
            int ocupadas = agendamentoRepository.contarVagasOcupadasBySituacoes(usuario, data, TurnoEnum.TARDE,
                    ocupantesVaga);
            vagasDisponiveisTotal += Math.max(0, tarde.getCapacidade() - ocupadas);
        }

        return vagasDisponiveisTotal;
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

        // 1) Buscar disponibilidades do usuário (todas) e montar map: DayOfWeek ->
        // Disponibilidade por turno
        List<Disponibilidade> todasDisponibilidades = disponibilidadeRepository.findByUsuario(usuario);
        Map<DayOfWeek, Map<TurnoEnum, Disponibilidade>> disponibilidadeMap = new HashMap<>();
        for (Disponibilidade d : todasDisponibilidades) {
            disponibilidadeMap
                    .computeIfAbsent(d.getDiaDaSemana(), k -> new HashMap<>())
                    .put(d.getTurno(), d);
        }

        // 2) Buscar bloqueios do usuário (em lote) e gerar um conjunto de datas
        // bloqueadas dentro do intervalo
        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findByUsuario(usuario);
        Set<LocalDate> datasBloqueadas = new HashSet<>();
        for (BloqueioAgenda b : bloqueios) {
            LocalDate start = b.getDataInicio();
            LocalDate end = b.getDataFim();
            if (end == null && start == null)
                continue;
            // Intersecta com nosso intervalo de busca
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

        // 3) Buscar agendamentos do usuário no intervalo (em lote) e agrupar por
        // data+turno somente considerando status ocupantes
        List<Agendamento> agendamentosNoIntervalo = agendamentoRepository
                .findByUsuarioAndDataAgendamentoBetween(usuario, primeiraData, ultimaData);
        // status que ocupam vaga
        List<SituacaoAtendimento> ocupantes = List.of(SituacaoAtendimento.AGENDADO, SituacaoAtendimento.REMARCADO,
                SituacaoAtendimento.PRESENTE);

        // Map<LocalDate, Map<TurnoEnum, Integer>> ocupacaoMap
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

        // 4) Iterar dias do intervalo e decidir se há vaga: usar disponibilidadeMap,
        // datasBloqueadas e ocupacaoMap
        LocalDate dataVerificacao = primeiraData;
        int diasBuscados = 0;

        while (datasDisponiveis.size() < quantidadeDesejada && diasBuscados < limiteDiasBusca) {
            diasBuscados++;

            if (dataVerificacao.isAfter(ultimaData))
                break;

            // 4.a) verificar bloqueio
            if (datasBloqueadas.contains(dataVerificacao)) {
                dataVerificacao = dataVerificacao.plusDays(1);
                continue;
            }

            // 4.b) verificar disponibilidade do dia da semana para o turno
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

            // 4.c) calcular vagas ocupadas pela consulta em memória
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
    public List<AgendamentoDTO> buscarAgendaDoDia(LocalDate data, String emailLogado, Long usuarioId) {
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        List<Agendamento> agendamentos;

        if (usuarioLogado.getTipoUsuario() == TipoUsuario.PROFISSIONAL) {
            agendamentos = agendamentoRepository.findByDataAgendamentoAndUsuario(data, usuarioLogado);
        } else {
            if (usuarioId != null) {
                Usuario profissionalAlvo = usuarioRepository.findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

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

        // só permitir registrar presença/falta para agendamentos com data <= hoje
        LocalDate dataAgendamento = agendamento.getDataAgendamento();
        if (dataAgendamento != null && dataAgendamento.isAfter(LocalDate.now())) {
            throw new ValidationException(
                    "Não é permitido registrar presença ou falta para agendamentos com data futura.");
        }

        // Controle otimista: se cliente informou expectedVersion, verifica se bate com
        // versão atual.
        if (expectedVersion != null && !expectedVersion.equals(agendamento.getVersion())) {
            throw new OptimisticLockException(
                    "O agendamento foi alterado por outro usuário. Atualize a agenda antes de tentar novamente.");
        }

        agendamento.setSituacaoAtendimento(novoStatus);

        Paciente paciente = agendamento.getPaciente();
        if (paciente != null) {
            if (novoStatus == SituacaoAtendimento.FALTOU) {
                paciente.setCountFaltas(paciente.getCountFaltas() + 1);
            } else if (novoStatus == SituacaoAtendimento.PRESENTE) {
                paciente.setCountFaltas(0);
                paciente.setDataUltimaPresenca(LocalDate.now());
            }
            // salva paciente após receber falta ou returar falta ao atualizar o status do
            // agendamento
            pacienteRepository.save(paciente);
        }

        agendamento = agendamentoRepository.save(agendamento);

        return agendamentoMapper.toAgendamentoDTO(agendamento);
    }
}
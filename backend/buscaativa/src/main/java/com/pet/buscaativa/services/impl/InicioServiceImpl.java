package com.pet.buscaativa.services.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.buscaativa.entities.dto.PacientePrioritarioDTO;
import com.pet.buscaativa.entities.dto.ResumoInicioDTO;
import com.pet.buscaativa.entities.enums.ClassificacaoRisco;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.StatusPaciente;
import com.pet.buscaativa.repositories.AgendamentoRepository;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.services.InicioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InicioServiceImpl implements InicioService {

    private final PacienteRepository pacienteRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Override
    @Transactional(readOnly = true)
    public ResumoInicioDTO buscarResumo() {
        LocalDate hoje = LocalDate.now();
        LocalDate seteDiasAtras = hoje.minusDays(6);

        long totalAtivos = pacienteRepository.countByStatusPaciente(StatusPaciente.ATIVO);

        long agendamentosHoje = agendamentoRepository.countByDataAgendamentoAndSituacaoAtendimento(hoje, SituacaoAtendimento.AGENDADO);

        long faltasUltimosSeteDias = agendamentoRepository.countBySituacaoAtendimentoAndDataAgendamentoBetween(SituacaoAtendimento.FALTOU, seteDiasAtras, hoje);

        long pacientesAtencao = pacienteRepository.countByStatusPacienteAndClassificacaoRiscoIn(StatusPaciente.ATIVO, List.of(ClassificacaoRisco.AMARELO, ClassificacaoRisco.VERMELHO));

        List<PacientePrioritarioDTO> prioritarios = pacienteRepository
                .findPacientesPrioritarios(StatusPaciente.ATIVO,
                        List.of(ClassificacaoRisco.VERMELHO, ClassificacaoRisco.AMARELO), PageRequest.of(0, 5))
                .stream()
                .map(p -> new PacientePrioritarioDTO(
                        p.getIdPublico(),
                        p.getNome(),
                        p.getClassificacaoRisco(),
                        p.getCountFaltas(),
                        p.getDataUltimaPresenca()))
                .toList();

        return new ResumoInicioDTO(
                totalAtivos,
                agendamentosHoje,
                faltasUltimosSeteDias,
                pacientesAtencao,
                pacientesAtencao,
                prioritarios);
    }
}
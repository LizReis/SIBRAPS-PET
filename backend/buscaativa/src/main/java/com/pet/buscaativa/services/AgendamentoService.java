package com.pet.buscaativa.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.AgendamentoDTO;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.TurnoEnum;

public interface AgendamentoService {

    AgendamentoDTO save(AgendamentoDTO agendamentoDTO);

    List<LocalDate> sugerirDataRemarcacao(Long agendamento);

    Map<TurnoEnum, Integer> calcularVagasDisponiveis(UUID usuarioIdPublico, LocalDate data);

    List<AgendamentoDTO> findAll();

    List<LocalDate> buscarProximasVagasDisponiveis(Usuario usuario, TurnoEnum turno, LocalDate dataInicio, int quantidadeVagas);

    List<AgendamentoDTO> buscarAgendaDoDia(LocalDate data, String emailLogado, UUID profissionalIdPublico);

    AgendamentoDTO atualizarStatus(Long id, SituacaoAtendimento novoStatus, Integer expectedVersion);
}
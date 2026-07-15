package com.pet.buscaativa.services;

import java.time.LocalDate;
import java.util.List;

import com.pet.buscaativa.entities.dto.AgendamentoDTO;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.TurnoEnum;

public interface AgendamentoService {


    AgendamentoDTO save(AgendamentoDTO agendamentoDTO);

    List<LocalDate> sugerirDataRemarcacao(Long agendamento);

    int calcularVagasDisponiveis(Long usuarioId, LocalDate data);

    List<AgendamentoDTO> findAll();

    List<LocalDate> buscarProximasVagasDisponiveis(Usuario usuario, TurnoEnum turno, LocalDate dataInicio, int quantidadeVagas);

    List<AgendamentoDTO> buscarAgendaDoDia(LocalDate data, String emailLogado, Long profissionalId);

    // Agora aceita expectedVersion para controle otimista (pode ser nulo)
    AgendamentoDTO atualizarStatus(Long id, SituacaoAtendimento novoStatus, Integer expectedVersion);

}
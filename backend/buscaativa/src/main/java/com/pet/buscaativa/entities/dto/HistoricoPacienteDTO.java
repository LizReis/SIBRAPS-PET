package com.pet.buscaativa.entities.dto;

import java.util.List;
import java.util.UUID;

import com.pet.buscaativa.entities.enums.StatusPaciente;

public record HistoricoPacienteDTO(
        UUID pacienteId,
        String nomePaciente,
        StatusPaciente situacaoAtual,
        List<HistoricoPacienteEventoDTO> eventos) {
}
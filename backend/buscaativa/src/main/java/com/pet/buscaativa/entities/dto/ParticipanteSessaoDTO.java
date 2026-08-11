package com.pet.buscaativa.entities.dto;

import java.util.UUID;

public record ParticipanteSessaoDTO(
        UUID pacienteId,
        String nomePaciente
) {
}
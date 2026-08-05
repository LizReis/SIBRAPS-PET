package com.pet.buscaativa.entities.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record BloqueioAgendaDTO(
        Long id,

        UUID usuarioId,

        @NotNull
        LocalDate dataInicio,

        @NotNull
        LocalDate dataFim,

        @NotBlank(message = "Informe o motivo do bloqueio.")
        String motivoBloqueio
) {
}
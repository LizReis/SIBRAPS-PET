package com.pet.buscaativa.entities.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record BloqueioAgendaDTO(
        Long id,

        Long usuarioId,

        @NotNull 
        LocalDate dataInicio,

        @NotNull 
        LocalDate dataFim,

        String motivoBloqueio
) {}
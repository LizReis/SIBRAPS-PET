package com.pet.buscaativa.entities.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.TurnoEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AgendamentoDTO(

    Long id,

    @NotBlank
    @NotNull
    Long usuarioId,

    @NotBlank
    @NotNull
    Long pacienteId,

    @NotBlank
    @NotNull(message = "Defina uma data para o Atendimento.")
    LocalDate dataAgendamento,

    @NotBlank
    @NotNull(message = "Defina o turno do Atendimento.")
    TurnoEnum turnoAgendamento,

    @NotBlank
    @NotNull(message = "Defina a hora do Atendimento.")
    LocalTime horaAtendimento,

    SituacaoAtendimento situacaoAtendimento,

    Integer version
) {
    
}

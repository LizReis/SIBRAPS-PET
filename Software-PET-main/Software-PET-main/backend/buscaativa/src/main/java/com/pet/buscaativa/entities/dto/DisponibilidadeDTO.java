package com.pet.buscaativa.entities.dto;

import java.time.DayOfWeek;

import com.pet.buscaativa.entities.enums.TurnoEnum;
import com.pet.buscaativa.validation.UniqueDisponibilidade;

import jakarta.validation.constraints.NotNull;

@UniqueDisponibilidade
public record DisponibilidadeDTO(

        Long id,

        Long usuarioId,

        @NotNull 
        DayOfWeek diaSemana,

        @NotNull 
        TurnoEnum turno,

        @NotNull 
        Integer capacidade
        
) {

}
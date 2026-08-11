package com.pet.buscaativa.entities.dto;

import java.time.LocalTime;
import java.util.UUID;

import com.pet.buscaativa.entities.enums.RecorrenciaGrupo;

public record GrupoTerapeuticoDTO(
        Long id,
        String tema,
        UUID coordenadorId,
        String nomeCoordenador,
        RecorrenciaGrupo recorrencia,
        LocalTime horarioPadrao,
        boolean ativo
) {
}
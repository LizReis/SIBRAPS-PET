package com.pet.buscaativa.entities.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.pet.buscaativa.entities.enums.StatusSessaoGrupo;

public record SessaoGrupoDTO(
        Long id,
        Long grupoId,
        String temaGrupo,
        String nomeCoordenador,
        LocalDate dataSessao,
        LocalTime horario,
        StatusSessaoGrupo status,
        List<ParticipanteSessaoDTO> participantes,
        Integer quantidadeParticipantes,
        Integer version
) {
}
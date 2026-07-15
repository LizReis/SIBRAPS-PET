package com.pet.buscaativa.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.pet.buscaativa.entities.Agendamento;
import com.pet.buscaativa.entities.dto.AgendamentoDTO;

@Mapper(componentModel = "spring")
public interface AgendamentoMapper {

    public AgendamentoDTO toAgendamentoDTO(Agendamento agendamentoEntity);

    public Agendamento toAgendamentoEntity(AgendamentoDTO agendamentoDTO);

    void updateAgendamentoFromDTO(AgendamentoDTO agendamentoDTO, @MappingTarget Agendamento agendamentoEntity);    
}
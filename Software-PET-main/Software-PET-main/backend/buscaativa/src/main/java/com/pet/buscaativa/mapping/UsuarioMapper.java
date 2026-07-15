package com.pet.buscaativa.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.UsuarioDTO;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "senha", ignore = true)
    public UsuarioDTO toUsuarioDTO(Usuario usuarioEntity);

    @Mapping(target = "idPublico", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "id", ignore = true)
    public Usuario toUsuarioEntity(UsuarioDTO usuarioDTO);
}
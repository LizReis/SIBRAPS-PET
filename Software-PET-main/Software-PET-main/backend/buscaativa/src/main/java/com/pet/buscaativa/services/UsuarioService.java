package com.pet.buscaativa.services;

import com.pet.buscaativa.entities.dto.UsuarioDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface UsuarioService {

    UsuarioDTO save(UsuarioDTO usuarioDTO);

    void removerUsuario(UUID idPublico);

    public List<UsuarioDTO> findAll();

    public UsuarioDTO findById(UUID idPublico);

    public UsuarioDTO findByEmail(String email);

    public void validarEmailDuplicado(String email, UUID idPublico);

}

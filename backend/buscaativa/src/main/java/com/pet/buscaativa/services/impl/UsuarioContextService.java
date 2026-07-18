package com.pet.buscaativa.services.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.enums.TipoUsuario;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioContextService {

    private final UsuarioRepository usuarioRepository;

    public Usuario determinarUsuarioAlvo(UUID usuarioIdPublico, String emailLogado) {
        if (emailLogado == null || emailLogado.isBlank()) {
            throw new ResourceNotFoundException("Usuário logado não identificado (email ausente).");
        }

        Usuario logado = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário logado não encontrado: " + emailLogado));

        if (usuarioIdPublico != null && logado.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return usuarioRepository.findByIdPublico(usuarioIdPublico)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário alvo não encontrado para id: " + usuarioIdPublico));
        }

        return logado;
    }
}
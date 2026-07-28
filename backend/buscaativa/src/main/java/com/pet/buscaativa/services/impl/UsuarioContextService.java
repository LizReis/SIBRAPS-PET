package com.pet.buscaativa.services.impl;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.enums.TipoUsuario;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;
import com.pet.buscaativa.services.exceptions.ValidationException;

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

        if(usuarioIdPublico != null && (logado.getTipoUsuario() == TipoUsuario.ADMINISTRADOR || logado.getTipoUsuario() == TipoUsuario.RECEPCAO)) {
            Usuario alvo = usuarioRepository.findByIdPublico(usuarioIdPublico)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário alvo não encontrado para id: " + usuarioIdPublico));
            if (alvo.getTipoUsuario() != TipoUsuario.PROFISSIONAL) {
                throw new ValidationException("O usuário informado não é um profissional.");
            }
            return alvo;
        }

        if (usuarioIdPublico != null && !usuarioIdPublico.equals(logado.getIdPublico())) {
            throw new AccessDeniedException("Acesso a recurso de outro profissional.");
        }
        
        return logado;
    }

    public void validarAlteracao(Usuario proprietario, String emailLogado) {
        Usuario logado = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
        if (logado.getTipoUsuario() != TipoUsuario.ADMINISTRADOR
                && !logado.getId().equals(proprietario.getId())) {
            throw new AccessDeniedException("Você não pode alterar este recurso.");
        }
    }
}
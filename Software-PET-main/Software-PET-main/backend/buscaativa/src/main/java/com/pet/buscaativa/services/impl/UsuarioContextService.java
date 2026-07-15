package com.pet.buscaativa.services.impl;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.enums.TipoUsuario;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Serviço utilitário para determinar o usuário alvo de operações administrativas.
 * Centraliza a regra:
 *  - Se usuarioId for informado e o usuário logado for ADMINISTRADOR -> retorna o usuário indicado.
 *  - Caso contrário, retorna o usuário logado (identificado por email).
 */
@Service
@RequiredArgsConstructor
public class UsuarioContextService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Determina o usuário alvo com base no usuarioId (pode ser null) e no email do usuário logado.
     *
     * @param usuarioId   id do usuário alvo (pode ser null)
     * @param emailLogado email do usuário logado (não nulo)
     * @return instância de Usuario (persistente)
     * @throws ResourceNotFoundException se usuário logado ou usuário alvo não existir
     */
    public Usuario determinarUsuarioAlvo(Long usuarioId, String emailLogado) {
        if (emailLogado == null || emailLogado.isBlank()) {
            throw new ResourceNotFoundException("Usuário logado não identificado (email ausente).");
        }

        Usuario logado = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário logado não encontrado: " + emailLogado));

        if (usuarioId != null && logado.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário alvo não encontrado para id: " + usuarioId));
        }

        return logado;
    }
}
package com.pet.buscaativa.services.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.DisponibilidadeExcecao;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.DisponibilidadeExcecaoDTO;
import com.pet.buscaativa.repositories.DisponibilidadeExcecaoRepository;
import com.pet.buscaativa.services.DisponibilidadeExcecaoService;
import com.pet.buscaativa.services.exceptions.RecursoDuplicadoException;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DisponibilidadeExcecaoServiceImpl implements DisponibilidadeExcecaoService {

    private final DisponibilidadeExcecaoRepository excecaoRepository;
    private final UsuarioContextService usuarioContextService;

    @Override
    public DisponibilidadeExcecaoDTO save(DisponibilidadeExcecaoDTO dto, String emailLogado) {
        Usuario usuario = usuarioContextService.determinarUsuarioAlvo(dto.usuarioId(), emailLogado);

        Optional<DisponibilidadeExcecao> existente = excecaoRepository
                .findByUsuarioAndDataAndTurno(usuario, dto.data(), dto.turno());

        DisponibilidadeExcecao excecao;

        if (existente.isPresent()) {
            excecao = existente.get();
            // Se veio um id diferente do já cadastrado, é duplicidade real
            if (dto.id() != null && !excecao.getId().equals(dto.id())) {
                throw new RecursoDuplicadoException(
                        "Já existe uma configuração para esta data e turno. Edite a existente.");
            }
            // Sem id no DTO mas já existe -> tratamos como "atualizar a existente"
            // (evita duplicidade e permite o usuário simplesmente reenviar a mesma data/turno com nova capacidade)
        } else if (dto.id() != null) {
            excecao = excecaoRepository.findById(dto.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Configuração não encontrada."));
        } else {
            excecao = new DisponibilidadeExcecao();
        }

        excecao.setUsuario(usuario);
        excecao.setData(dto.data());
        excecao.setTurno(dto.turno());
        excecao.setCapacidade(dto.capacidade());

        excecao = excecaoRepository.save(excecao);

        return toDTO(excecao);
    }

    @Override
    public List<DisponibilidadeExcecaoDTO> listar(String emailLogado, UUID usuarioIdPublico) {
        Usuario usuarioAlvo = usuarioContextService.determinarUsuarioAlvo(usuarioIdPublico, emailLogado);

        return excecaoRepository.findByUsuario(usuarioAlvo).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void deletar(Long id) {
        excecaoRepository.deleteById(id);
    }

    private DisponibilidadeExcecaoDTO toDTO(DisponibilidadeExcecao e) {
        return new DisponibilidadeExcecaoDTO(
                e.getId(),
                e.getUsuario().getIdPublico(),
                e.getData(),
                e.getTurno(),
                e.getCapacidade()
        );
    }
}
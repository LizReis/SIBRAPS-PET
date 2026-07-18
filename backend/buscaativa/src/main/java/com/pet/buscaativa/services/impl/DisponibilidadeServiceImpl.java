package com.pet.buscaativa.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Disponibilidade;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.DisponibilidadeDTO;
import com.pet.buscaativa.repositories.DisponibilidadeRepository;
import com.pet.buscaativa.services.DisponibilidadeService;
import com.pet.buscaativa.services.exceptions.RecursoDuplicadoException;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisponibilidadeServiceImpl implements DisponibilidadeService {

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final UsuarioContextService usuarioContextService;

    @Override
    public DisponibilidadeDTO save(DisponibilidadeDTO disponibilidadeDTO, String emailLogado) {
        Usuario usuario = usuarioContextService.determinarUsuarioAlvo(disponibilidadeDTO.usuarioId(), emailLogado);

        Optional<Disponibilidade> checarDisponibilidade = disponibilidadeRepository
                .findByUsuarioAndDiaDaSemanaAndTurno(usuario, disponibilidadeDTO.diaSemana(), disponibilidadeDTO.turno());

        if (checarDisponibilidade.isPresent()) {
            Disponibilidade existente = checarDisponibilidade.get();
            if (disponibilidadeDTO.id() == null || !existente.getId().equals(disponibilidadeDTO.id())) {
                throw new RecursoDuplicadoException("Já existe disponibilidade cadastrada para este profissional, dia da semana e turno.");
            }
        }

        Disponibilidade disponibilidade = new Disponibilidade();
        if (disponibilidadeDTO.id() != null) {
            disponibilidade = disponibilidadeRepository.findById(disponibilidadeDTO.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Disponibilidade não encontrada."));
        }

        disponibilidade.setUsuario(usuario);
        disponibilidade.setDiaDaSemana(disponibilidadeDTO.diaSemana());
        disponibilidade.setTurno(disponibilidadeDTO.turno());
        disponibilidade.setCapacidade(disponibilidadeDTO.capacidade());

        disponibilidade = disponibilidadeRepository.save(disponibilidade);

        return toDTO(disponibilidade);
    }

    @Override
    public List<DisponibilidadeDTO> listarDisponibilidades(String emailLogado, UUID usuarioIdPublico) {
        Usuario usuarioAlvo = usuarioContextService.determinarUsuarioAlvo(usuarioIdPublico, emailLogado);

        return disponibilidadeRepository.findByUsuario(usuarioAlvo).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void deletarDisponibilidade(Long id) {
        disponibilidadeRepository.deleteById(id);
    }

    private DisponibilidadeDTO toDTO(Disponibilidade d) {
        return new DisponibilidadeDTO(
                d.getId(),
                d.getUsuario().getIdPublico(),
                d.getDiaDaSemana(),
                d.getTurno(),
                d.getCapacidade()
        );
    }
}
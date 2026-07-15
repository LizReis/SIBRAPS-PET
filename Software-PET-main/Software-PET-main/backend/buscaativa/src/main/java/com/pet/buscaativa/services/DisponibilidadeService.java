package com.pet.buscaativa.services;

import java.util.List;

import com.pet.buscaativa.entities.dto.DisponibilidadeDTO;

public interface DisponibilidadeService {

    DisponibilidadeDTO save(DisponibilidadeDTO disponibilidadeDTO, String emailLogado);

    List<DisponibilidadeDTO> listarDisponibilidades(String emailLogado, Long usuarioId);

    void deletarDisponibilidade(Long id);
}
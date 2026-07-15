package com.pet.buscaativa.services;

import java.util.List;

import com.pet.buscaativa.entities.dto.BloqueioAgendaDTO;

public interface BloqueioAgendaService {

    BloqueioAgendaDTO save(BloqueioAgendaDTO bloqueioAgendaDTO, String emailLogado);

    List<BloqueioAgendaDTO> listarBloqueios(String emailLogado, Long usuarioId);

    void deletarBloqueio(Long id);
}
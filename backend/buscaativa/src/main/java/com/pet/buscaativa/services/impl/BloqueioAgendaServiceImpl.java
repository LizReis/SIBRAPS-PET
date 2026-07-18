package com.pet.buscaativa.services.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.BloqueioAgenda;
import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.BloqueioAgendaDTO;
import com.pet.buscaativa.repositories.BloqueioAgendaRepository;
import com.pet.buscaativa.services.BloqueioAgendaService;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BloqueioAgendaServiceImpl implements BloqueioAgendaService {

    private final BloqueioAgendaRepository bloqueioRepository;
    private final UsuarioContextService usuarioContextService;

    @Override
    public BloqueioAgendaDTO save(BloqueioAgendaDTO bloqueioAgendaDTO, String emailLogado) {
        Usuario usuario = usuarioContextService.determinarUsuarioAlvo(bloqueioAgendaDTO.usuarioId(), emailLogado);

        BloqueioAgenda bloqueio = new BloqueioAgenda();
        if (bloqueioAgendaDTO.id() != null) {
            bloqueio = bloqueioRepository.findById(bloqueioAgendaDTO.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Bloqueio não encontrado."));
        }

        bloqueio.setUsuario(usuario);
        bloqueio.setDataInicio(bloqueioAgendaDTO.dataInicio());
        bloqueio.setDataFim(bloqueioAgendaDTO.dataFim());
        bloqueio.setMotivoBloqueio(bloqueioAgendaDTO.motivoBloqueio());

        bloqueio = bloqueioRepository.save(bloqueio);

        return toDTO(bloqueio);
    }

    @Override
    public List<BloqueioAgendaDTO> listarBloqueios(String emailLogado, UUID usuarioIdPublico) {
        Usuario usuarioAlvo = usuarioContextService.determinarUsuarioAlvo(usuarioIdPublico, emailLogado);

        return bloqueioRepository.findByUsuario(usuarioAlvo).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void deletarBloqueio(Long id) {
        bloqueioRepository.deleteById(id);
    }

    private BloqueioAgendaDTO toDTO(BloqueioAgenda b) {
        return new BloqueioAgendaDTO(
                b.getId(),
                b.getUsuario().getIdPublico(),
                b.getDataInicio(),
                b.getDataFim(),
                b.getMotivoBloqueio()
        );
    }
}
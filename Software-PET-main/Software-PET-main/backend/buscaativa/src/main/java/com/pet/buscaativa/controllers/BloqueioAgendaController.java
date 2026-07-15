package com.pet.buscaativa.controllers;

import com.pet.buscaativa.entities.dto.BloqueioAgendaDTO;
import com.pet.buscaativa.entities.dto.DisponibilidadeDTO;
import com.pet.buscaativa.services.BloqueioAgendaService;
import com.pet.buscaativa.services.DisponibilidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bloqueioAgenda-config")
@RequiredArgsConstructor
public class BloqueioAgendaController {

    private final BloqueioAgendaService bloqueioAgendaService;
    
    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    //Salva um bloqueio de agenda
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL')")
    @PostMapping
    public ResponseEntity<BloqueioAgendaDTO> salvarBloqueio(@RequestBody @Valid BloqueioAgendaDTO bloqueioAgendaDTO) {
        return ResponseEntity.ok(bloqueioAgendaService.save(bloqueioAgendaDTO, getEmailLogado()));
    }

    //Lista os bloqueios de um profissional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @GetMapping
    public ResponseEntity<List<BloqueioAgendaDTO>> listarBloqueios(@RequestParam(required = false) Long usuarioId) {
        return ResponseEntity.ok(bloqueioAgendaService.listarBloqueios(getEmailLogado(), usuarioId));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarBloqueio(@PathVariable Long id) {
        bloqueioAgendaService.deletarBloqueio(id);
        return ResponseEntity.noContent().build();
    }
}
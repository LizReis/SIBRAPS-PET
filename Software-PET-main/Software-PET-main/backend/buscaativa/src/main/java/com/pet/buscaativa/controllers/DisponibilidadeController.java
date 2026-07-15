package com.pet.buscaativa.controllers;

import com.pet.buscaativa.entities.dto.BloqueioAgendaDTO;
import com.pet.buscaativa.entities.dto.DisponibilidadeDTO;
import com.pet.buscaativa.services.DisponibilidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilidade-config")
@RequiredArgsConstructor
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidadeService;
    
    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    //Salva disponibilidade
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL')")
    @PostMapping("/disponibilidade")
    public ResponseEntity<DisponibilidadeDTO> salvarDisponibilidade(@RequestBody @Valid DisponibilidadeDTO disponibilidadeDTO) {
        return ResponseEntity.ok(disponibilidadeService.save(disponibilidadeDTO, getEmailLogado()));
    }

    //Lista a disponibilidade 
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @GetMapping("/disponibilidade")
    public ResponseEntity<List<DisponibilidadeDTO>> listarDisponibilidades(@RequestParam(required = false) Long usuarioId) {
        return ResponseEntity.ok(disponibilidadeService.listarDisponibilidades(getEmailLogado(), usuarioId));
    }

    //Deleta uma disponibilidade
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL')")
    @DeleteMapping("/disponibilidade/{id}")
    public ResponseEntity<Void> deletarDisponibilidade(@PathVariable Long id) {
        disponibilidadeService.deletarDisponibilidade(id);
        return ResponseEntity.noContent().build();
    }
}
package com.pet.buscaativa.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.pet.buscaativa.entities.dto.AdicionarParticipanteDTO;
import com.pet.buscaativa.entities.dto.CriarGrupoDTO;
import com.pet.buscaativa.entities.dto.GrupoTerapeuticoDTO;
import com.pet.buscaativa.entities.dto.NovaSessaoDTO;
import com.pet.buscaativa.entities.dto.SessaoGrupoDTO;
import com.pet.buscaativa.entities.enums.StatusSessaoGrupo;
import com.pet.buscaativa.services.GrupoTerapeuticoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
public class GrupoTerapeuticoController {

    private final GrupoTerapeuticoService grupoService;

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @PostMapping
    public ResponseEntity<GrupoTerapeuticoDTO> criarGrupo(@RequestBody @Valid CriarGrupoDTO dto) {
        return ResponseEntity.ok(grupoService.criarGrupo(dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @GetMapping
    public ResponseEntity<List<GrupoTerapeuticoDTO>> listarGrupos() {
        return ResponseEntity.ok(grupoService.listarGrupos());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @GetMapping("/{id}/proxima-data-sugerida")
    public ResponseEntity<LocalDate> sugerirProximaData(@PathVariable Long id) {
        return ResponseEntity.ok(grupoService.sugerirProximaData(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @PostMapping("/sessoes")
    public ResponseEntity<SessaoGrupoDTO> criarProximaSessao(@RequestBody @Valid NovaSessaoDTO dto) {
        return ResponseEntity.ok(grupoService.criarProximaSessao(dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @GetMapping("/sessoes")
    public ResponseEntity<List<SessaoGrupoDTO>> listarSessoes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(grupoService.listarSessoes(dataInicio, dataFim));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @PostMapping("/sessoes/{sessaoId}/participantes")
    public ResponseEntity<SessaoGrupoDTO> adicionarParticipante(@PathVariable Long sessaoId,
                                                                @RequestBody @Valid AdicionarParticipanteDTO dto) {
        return ResponseEntity.ok(grupoService.adicionarParticipante(sessaoId, dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @DeleteMapping("/sessoes/{sessaoId}/participantes/{pacienteId}")
    public ResponseEntity<SessaoGrupoDTO> removerParticipante(@PathVariable Long sessaoId,
                                                              @PathVariable UUID pacienteId) {
        return ResponseEntity.ok(grupoService.removerParticipante(sessaoId, pacienteId));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @PatchMapping("/sessoes/{sessaoId}/status")
    public ResponseEntity<SessaoGrupoDTO> atualizarStatus(@PathVariable Long sessaoId,
                                                          @RequestParam StatusSessaoGrupo novoStatus,
                                                          @RequestParam(required = false) Integer version) {
        return ResponseEntity.ok(grupoService.atualizarStatus(sessaoId, novoStatus, version));
    }
}
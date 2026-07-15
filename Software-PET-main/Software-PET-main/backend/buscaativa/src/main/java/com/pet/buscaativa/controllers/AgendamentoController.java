package com.pet.buscaativa.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pet.buscaativa.entities.dto.AgendamentoDTO;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.services.AgendamentoService;
import com.pet.buscaativa.services.exceptions.ValidationException;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {
    
    private final AgendamentoService agendamentoService;

    //Cria agendamento
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCAO')")
    @PostMapping
    public ResponseEntity<?> criarAgendamento(@RequestBody @Valid AgendamentoDTO agendamentoDTO) {
        try {
            AgendamentoDTO agendamentoCriado = agendamentoService.save(agendamentoDTO);

            java.net.URI uri = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(agendamentoCriado.id()).toUri();
                    
            return ResponseEntity.created(uri).body(agendamentoCriado);
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // manter comportamento anterior para outras exceções (poderá ser centralizado no GlobalExceptionHandler posteriormente)
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno"));
        }
    }

    //Sugere 3 datas para Remarcar Agendamento como foi pedido nos requisitos
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCAO')")
    @GetMapping("/{id}/remarcacao")
    public ResponseEntity<?> sugerirRemarcacao(@PathVariable Long id) {
        try {
            List<LocalDate> sugestoes = agendamentoService.sugerirDataRemarcacao(id);
            return ResponseEntity.ok(sugestoes);
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //Busca a agenda do dia do progissional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @GetMapping("/agenda/{data}")
    public ResponseEntity<List<AgendamentoDTO>> buscarAgendaDoDia(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data, @RequestParam(required = false) Long profissionalId ){
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        return ResponseEntity.ok(agendamentoService.buscarAgendaDoDia(data, emailLogado, profissionalId));
    }

    //Atualiza o status do agendamento
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO')")
    @GetMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable Long id, @RequestParam SituacaoAtendimento novoStatus, @RequestParam(required = false) Integer version) {
        try {
            AgendamentoDTO agendamentoAtualizado = agendamentoService.atualizarStatus(id, novoStatus, version);
            return ResponseEntity.ok(agendamentoAtualizado);
        } catch (OptimisticLockException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

package com.pet.buscaativa.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pet.buscaativa.entities.dto.PacienteDTO;
import com.pet.buscaativa.services.PacienteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    public ResponseEntity<List<PacienteDTO>> listaPacientes(){
        return ResponseEntity.ok(pacienteService.findAll());
    }

    @GetMapping("/{idPublico}")
    public ResponseEntity<PacienteDTO> buscaPacienteId(@PathVariable UUID idPublico){
        return ResponseEntity.ok(pacienteService.findById(idPublico));
    }

    @GetMapping("/busca/cpf/{cpf}")
    public ResponseEntity<PacienteDTO> buscaPacienteCpf(@PathVariable String cpf){
        return ResponseEntity.ok(pacienteService.findByCpf(cpf));
    }

    @GetMapping("/busca/cns/{cns}")
    public ResponseEntity<PacienteDTO> buscaPacienteCns(@PathVariable String cns){
        return ResponseEntity.ok(pacienteService.findByCns(cns));
    }

    @GetMapping("/busca/nome")
    public ResponseEntity<List<PacienteDTO>> buscaPacienteNome(@RequestParam(value = "q", defaultValue = "") String nome){
        return ResponseEntity.ok(pacienteService.findByNome(nome));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<PacienteDTO> insert(@Valid @RequestBody PacienteDTO pacienteDTO, 
        @RequestParam(value = "ignorarSimilaridade", defaultValue = "false") boolean ignorarSimilaridade){
        
            PacienteDTO novoPacienteDTO = pacienteService.save(pacienteDTO, ignorarSimilaridade);

            URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(novoPacienteDTO.idPublico()).toUri();

            return ResponseEntity.created(uri).body(novoPacienteDTO);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{idPublico}")
    public ResponseEntity<PacienteDTO> update(@PathVariable UUID idPublico, @Valid @RequestBody PacienteDTO pacienteDTO, 
        @RequestParam(value = "ignorarSimilaridade", defaultValue = "false") boolean ignorarSimilaridade){

            PacienteDTO pacienteAtualizar = new PacienteDTO(idPublico, pacienteDTO.nome(), pacienteDTO.nomeMae(), pacienteDTO.dataNascimento(), pacienteDTO.dataUltimaPresenca(),
                pacienteDTO.sexo(), pacienteDTO.racacor(), pacienteDTO.cns(), pacienteDTO.cpf(), pacienteDTO.telefone(), pacienteDTO.endereco(),
                pacienteDTO.situacaoRua(), pacienteDTO.tipoAcompanhamento(), pacienteDTO.countFaltas(), pacienteDTO.statusPaciente(),
                pacienteDTO.usfReferencia(), pacienteDTO.capsReferencia());

                return ResponseEntity.ok(pacienteService.save(pacienteAtualizar, ignorarSimilaridade));
        }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{idPublico}")
    public ResponseEntity<Void> inativarPaciente(@PathVariable UUID idPublico){

        pacienteService.inativarPaciente(idPublico);
        return ResponseEntity.noContent().build();

    }
}

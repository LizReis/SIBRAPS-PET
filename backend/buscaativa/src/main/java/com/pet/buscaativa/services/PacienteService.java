package com.pet.buscaativa.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.dto.AlertaBuscaAtivaDTO;
import com.pet.buscaativa.entities.dto.EncerramentoPacienteDTO;
import com.pet.buscaativa.entities.dto.PacienteDTO;
import com.pet.buscaativa.entities.dto.ReativacaoPacienteDTO;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;

@Service
public interface PacienteService {

    PacienteDTO save(PacienteDTO pacienteDTO, boolean ignorarSimilaridade);

    List<PacienteDTO> findAll();

    PacienteDTO findById(UUID idPublico);

    PacienteDTO findByCns(String cns);

    PacienteDTO findByCpf(String cpf);

    List<PacienteDTO> findByNome(String nome);

    PacienteDTO findByNomeMae(String nomeMae);

    void inativarPaciente(UUID idPublico);

    void encerrarAcompanhamento(UUID idPublico, EncerramentoPacienteDTO encerramento);

    void reativarAcompanhamento(UUID idPublico, ReativacaoPacienteDTO reativacao);

    List<AlertaBuscaAtivaDTO> listarPacientesEmBuscaAtiva();
    
    void validarPacienteDuplicado(PacienteDTO pacienteDTO, boolean ignorarSimilaridade);

    void atualizarAssiduidadePaciente(Paciente paciente, SituacaoAtendimento statusAnterior, SituacaoAtendimento novoStatus);

    void calcularEAtualizarRisco(Paciente paciente);

}
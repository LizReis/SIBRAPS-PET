package com.pet.buscaativa.services;

import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.dto.PacienteDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
    void validarPacienteDuplicado(PacienteDTO pacienteDTO, boolean ignorarSimilaridade);

}
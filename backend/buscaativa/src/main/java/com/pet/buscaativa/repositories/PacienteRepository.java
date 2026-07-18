package com.pet.buscaativa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pet.buscaativa.entities.Paciente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pet.buscaativa.entities.enums.StatusPaciente;



@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long>{
 
    Optional<Paciente> findByIdPublico(UUID idPublico);

    Optional<Paciente> findByCns(String cns);

    Optional<Paciente> findByCpf(String cpf);

    Optional<Paciente> findByNome(String nome);

    Optional<Paciente> findByNomeMae(String nomeMae);

    List<Paciente> findByNomeContainingIgnoreCase (String nome);    

    List<Paciente> findByNomeIgnoreCaseAndNomeMaeIgnoreCaseAndDataNascimento(
    String nome, String nomeMae, LocalDate dataNascimento);

    List<Paciente> findByStatusPaciente(StatusPaciente statusPaciente);
}

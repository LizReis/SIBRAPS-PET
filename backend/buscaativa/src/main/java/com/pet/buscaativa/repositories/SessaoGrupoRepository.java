package com.pet.buscaativa.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pet.buscaativa.entities.GrupoTerapeutico;
import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.SessaoGrupo;
import com.pet.buscaativa.entities.enums.StatusSessaoGrupo;

@Repository
public interface SessaoGrupoRepository extends JpaRepository<SessaoGrupo, Long> {

    List<SessaoGrupo> findByGrupoOrderByDataSessaoDesc(GrupoTerapeutico grupo);

    Optional<SessaoGrupo> findFirstByGrupoOrderByDataSessaoDesc(GrupoTerapeutico grupo);

     @Query("SELECT DISTINCT s FROM SessaoGrupo s JOIN FETCH s.grupo g JOIN FETCH g.coordenador " +
            "LEFT JOIN FETCH s.participantes p LEFT JOIN FETCH p.paciente " +
            "WHERE s.dataSessao BETWEEN :dataInicio AND :dataFim " +
            "ORDER BY s.dataSessao, s.horario")
    List<SessaoGrupo> findByDataSessaoBetween(@Param("dataInicio") LocalDate dataInicio,
                                              @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(p) > 0 FROM SessaoGrupoParticipante p " +
            "WHERE p.paciente = :paciente AND p.sessaoGrupo.dataSessao = :data " +
            "AND p.sessaoGrupo.horario = :horario AND p.sessaoGrupo.status <> :statusCancelada")
    boolean existsParticipacaoNoMesmoHorario(@Param("paciente") Paciente paciente,
                                             @Param("data") LocalDate data, @Param("horario") LocalTime horario,
                                             @Param("statusCancelada") StatusSessaoGrupo statusCancelada);
}
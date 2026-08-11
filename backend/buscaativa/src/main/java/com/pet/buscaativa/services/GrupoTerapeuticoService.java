package com.pet.buscaativa.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.pet.buscaativa.entities.dto.AdicionarParticipanteDTO;
import com.pet.buscaativa.entities.dto.CriarGrupoDTO;
import com.pet.buscaativa.entities.dto.GrupoTerapeuticoDTO;
import com.pet.buscaativa.entities.dto.NovaSessaoDTO;
import com.pet.buscaativa.entities.dto.SessaoGrupoDTO;
import com.pet.buscaativa.entities.enums.StatusSessaoGrupo;

public interface GrupoTerapeuticoService {

    GrupoTerapeuticoDTO criarGrupo(CriarGrupoDTO dto);

    List<GrupoTerapeuticoDTO> listarGrupos();

    LocalDate sugerirProximaData(Long grupoId);

    SessaoGrupoDTO criarProximaSessao(NovaSessaoDTO dto);

    List<SessaoGrupoDTO> listarSessoes(LocalDate dataInicio, LocalDate dataFim);

    SessaoGrupoDTO adicionarParticipante(Long sessaoId, AdicionarParticipanteDTO dto);

    SessaoGrupoDTO removerParticipante(Long sessaoId, UUID pacienteId);

    SessaoGrupoDTO atualizarStatus(Long sessaoId, StatusSessaoGrupo novoStatus, Integer expectedVersion);
}
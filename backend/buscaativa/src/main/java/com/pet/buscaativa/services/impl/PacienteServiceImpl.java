package com.pet.buscaativa.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.dto.PacienteDTO;
import com.pet.buscaativa.entities.enums.ClassificacaoRisco;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.StatusPaciente;
import com.pet.buscaativa.entities.enums.TipoAcompanhamento;
import com.pet.buscaativa.mapping.PacienteMapper;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.services.PacienteService;
import com.pet.buscaativa.services.exceptions.DatabaseException;
import com.pet.buscaativa.services.exceptions.RecursoDuplicadoException;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;
import com.pet.buscaativa.utils.DocumentoUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService{

    private final PacienteRepository pacienteRepository;
    private final PacienteMapper pacienteMapper;


    @Override
    public PacienteDTO save(PacienteDTO pacienteDTO, boolean ignorarSimilaridade) {
        validarPacienteDuplicado(pacienteDTO, ignorarSimilaridade);

        Paciente pacienteSalvar;

        if(pacienteDTO.idPublico() != null){
            pacienteSalvar = pacienteRepository.findByIdPublico(pacienteDTO.idPublico())
                    .orElseThrow(() -> new DatabaseException("Paciente não encontrado!"));

            pacienteMapper.updatePacienteFromDTO(pacienteDTO, pacienteSalvar);
        }else{
            pacienteSalvar = pacienteMapper.toPacienteEntity(pacienteDTO);

            pacienteSalvar.setStatusPaciente(StatusPaciente.ATIVO);
        }

        pacienteSalvar = pacienteRepository.save(pacienteSalvar);

        return pacienteMapper.toPacienteDTO(pacienteSalvar);

    }
    @Override
    public void inativarPaciente(UUID idPublico) {
        Paciente paciente = pacienteRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> new DatabaseException("Paciente não encontrado!"));

        paciente.setStatusPaciente(StatusPaciente.INATIVO);

        pacienteRepository.save(paciente);
    }

    @Override
    public PacienteDTO findById(UUID idPublico) {
        Paciente paciente = pacienteRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> new ResourceNotFoundException(idPublico));
        return new PacienteDTO(paciente);
    }

    @Override
    public List<PacienteDTO> findAll() {
        List<Paciente> list = pacienteRepository.findAll();
        return list.stream()
                .map(PacienteDTO::new)
                .toList();
    }
    @Override
    public void validarPacienteDuplicado(PacienteDTO pacienteDTO, boolean ignorarSimilaridade) {
        boolean possuiDoc = false;

        if (pacienteDTO.cpf() != null && !pacienteDTO.cpf().isBlank()) {
            possuiDoc = true;
            String cpfNormalizado = DocumentoUtil.normalizarCPF(pacienteDTO.cpf());
            pacienteRepository.findByCpf(cpfNormalizado).ifPresent(p -> {
                if (!p.getId().equals(pacienteDTO.idPublico())) {
                    throw new RecursoDuplicadoException("Já existe um paciente com este CPF.");
                }
            });
        }

        if (pacienteDTO.cns() != null && !pacienteDTO.cns().isBlank()) {
            possuiDoc = true;
            String cnsNormalizado = DocumentoUtil.normalizarCNS(pacienteDTO.cns());
            pacienteRepository.findByCns(cnsNormalizado).ifPresent(p -> {
                if (!p.getId().equals(pacienteDTO.idPublico())) {
                    throw new RecursoDuplicadoException("Já existe um paciente com este CNS.");
                }
            });
        }

        //REGRA RF15: CPF/CNS ausentes -> Busca por similaridade
        if (!possuiDoc && !ignorarSimilaridade) {
            List<Paciente> similares = pacienteRepository
                .findByNomeIgnoreCaseAndNomeMaeIgnoreCaseAndDataNascimento(
                    pacienteDTO.nome(), pacienteDTO.nomeMae(), pacienteDTO.dataNascimento()
                );

                similares.removeIf(p -> p.getId().equals(pacienteDTO.idPublico()));

            if (!similares.isEmpty()) {
                List<PacienteDTO> similaresDTO = similares.stream().map(pacienteMapper::toPacienteDTO).toList();
                
                throw new DatabaseException(
                    "Registros similares encontrados. Confirme para prosseguir." + similaresDTO
                );
            }
        }
    }

    @Override
    public PacienteDTO findByCns(String cns) {
        String cnsNormalizado = DocumentoUtil.normalizarCNS(cns);
        Paciente paciente = pacienteRepository.findByCns(cnsNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com o CNS informado."));
        return pacienteMapper.toPacienteDTO(paciente);

    }

    @Override
    public PacienteDTO findByCpf(String cpf) {
        String cpfNormalizado = DocumentoUtil.normalizarCPF(cpf);
        Paciente paciente = pacienteRepository.findByCpf(cpfNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com o CPF informado."));
        return pacienteMapper.toPacienteDTO(paciente);

    }

    @Override
    public List<PacienteDTO> findByNome(String nome) {
        List<Paciente> pacientes = pacienteRepository.findByNomeContainingIgnoreCase(nome);
        
        return pacientes.stream()
                .map(pacienteMapper::toPacienteDTO)
                .toList();

    }

    @Override
    public PacienteDTO findByNomeMae(String nomeMae) {
        var paciente = pacienteRepository.findByNomeMae(nomeMae).orElse(null);

        if(paciente == null){
            return null;
        }

        return pacienteMapper.toPacienteDTO(paciente);
    }

    @Override
    public void atualizarAssiduidadePaciente(Paciente paciente, SituacaoAtendimento statusAnterior, SituacaoAtendimento novoStatus) {
        if (novoStatus == SituacaoAtendimento.FALTOU && statusAnterior != SituacaoAtendimento.FALTOU) {
            paciente.setCountFaltas(paciente.getCountFaltas() + 1);
        }
        if (novoStatus == SituacaoAtendimento.PRESENTE) {
            paciente.setCountFaltas(0);
            paciente.setDataUltimaPresenca(LocalDate.now());
            paciente.setGatilhoVisitaAcionado(false);
        }
        if (statusAnterior == SituacaoAtendimento.FALTOU && novoStatus != SituacaoAtendimento.FALTOU) {
            paciente.setCountFaltas(Math.max(0, paciente.getCountFaltas() - 1));
        }

        calcularEAtualizarRisco(paciente);
    }
    @Override
    public void calcularEAtualizarRisco(Paciente paciente) {
        if(paciente.getStatusPaciente() == StatusPaciente.INATIVO){
            paciente.setClassificacaoRisco(ClassificacaoRisco.VERDE);
            return;
        }

        //Definiçao dos limites padronizados de dias para Vermelho e Amarelo
        //Se o TipoAcompanhamento for Grupo terapeutico o amarelo recebe = 15, se for individual recebe 60
        //Se o TipoAcompanhamento for Grupo terapeutico o vermelho recebe = 30, se for individual recebe 120
        int limiteAmareloDias = paciente.getTipoAcompanhamento() == TipoAcompanhamento.GRUPO_TERAPEUTICO ? 15 : 60;
        int limiteVermelhoDias = paciente.getTipoAcompanhamento() == TipoAcompanhamento.GRUPO_TERAPEUTICO ? 30 : 120;
        int limiteAmareloFaltas = 2;
        int limiteVermelhoFaltas = 3;

        long diasAusente = 0;

        if (paciente.getDataUltimaPresenca() != null) {
            diasAusente = java.time.temporal.ChronoUnit.DAYS.between(paciente.getDataUltimaPresenca(), LocalDate.now());
        }

        // Verifica primeiro o risco máximo (Vermelho)
        if (paciente.getCountFaltas() >= limiteVermelhoFaltas || diasAusente >= limiteVermelhoDias) {
            paciente.setClassificacaoRisco(ClassificacaoRisco.VERMELHO);
        } 
        // Depois o risco médio (Amarelo)
        else if (paciente.getCountFaltas() >= limiteAmareloFaltas || diasAusente >= limiteAmareloDias) {
            paciente.setClassificacaoRisco(ClassificacaoRisco.AMARELO);
        } 
        // Caso contrário, está tudo bem (Verde)
        else {
            paciente.setClassificacaoRisco(ClassificacaoRisco.VERDE);
        }
    }

    
}
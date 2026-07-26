package com.pet.buscaativa.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.pet.buscaativa.entities.Endereco;
import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.dto.AlertaBuscaAtivaDTO;
import com.pet.buscaativa.entities.dto.EncerramentoPacienteDTO;
import com.pet.buscaativa.entities.dto.PacienteDTO;
import com.pet.buscaativa.entities.dto.ReativacaoPacienteDTO;
import com.pet.buscaativa.entities.enums.ClassificacaoRisco;
import com.pet.buscaativa.entities.enums.MotivoEncerramento;
import com.pet.buscaativa.entities.enums.SituacaoAtendimento;
import com.pet.buscaativa.entities.enums.StatusPaciente;
import com.pet.buscaativa.entities.enums.TipoAcompanhamento;
import com.pet.buscaativa.mapping.PacienteMapper;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.services.HistoricoPacienteService;
import com.pet.buscaativa.services.PacienteService;
import com.pet.buscaativa.services.exceptions.DatabaseException;
import com.pet.buscaativa.services.exceptions.RecursoDuplicadoException;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;
import com.pet.buscaativa.services.exceptions.ValidationException;
import com.pet.buscaativa.utils.DocumentoUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService{

    private final PacienteRepository pacienteRepository;
    private final PacienteMapper pacienteMapper;
    private final HistoricoPacienteService historicoPacienteService;


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

        pacienteSalvar.setCpf(
        DocumentoUtil.normalizarCPF(pacienteDTO.cpf()));

        pacienteSalvar.setCns(
        DocumentoUtil.normalizarCNS(pacienteDTO.cns()));


        pacienteSalvar = pacienteRepository.save(pacienteSalvar);

        return pacienteMapper.toPacienteDTO(pacienteSalvar);

    }
    @Override
    public void inativarPaciente(UUID idPublico) {
        Paciente paciente = pacienteRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> new DatabaseException("Paciente não encontrado!"));

        paciente.setStatusPaciente(StatusPaciente.INATIVO);

        pacienteRepository.save(paciente);
        historicoPacienteService.registrarSituacaoAtual(paciente, "Situação atualizada para INATIVO.");
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
    public void validarPacienteDuplicado(
            PacienteDTO pacienteDTO,
            boolean ignorarSimilaridade
    ) {
        boolean possuiDoc = false;

        if (pacienteDTO.cpf() != null && !pacienteDTO.cpf().isBlank()) {
            possuiDoc = true;

            String cpfNormalizado =
                    DocumentoUtil.normalizarCPF(pacienteDTO.cpf());

            pacienteRepository.findByCpf(cpfNormalizado).ifPresent(p -> {
                if (!Objects.equals(
                        p.getIdPublico(),
                        pacienteDTO.idPublico()
                )) {
                    throw new RecursoDuplicadoException(
                            "Já existe um paciente com este CPF."
                    );
                }
            });
        }

        if (pacienteDTO.cns() != null && !pacienteDTO.cns().isBlank()) {
            possuiDoc = true;

            String cnsNormalizado =
                    DocumentoUtil.normalizarCNS(pacienteDTO.cns());

            pacienteRepository.findByCns(cnsNormalizado).ifPresent(p -> {
                if (!Objects.equals(
                        p.getIdPublico(),
                        pacienteDTO.idPublico()
                )) {
                    throw new RecursoDuplicadoException(
                            "Já existe um paciente com este CNS."
                    );
                }
            });
        }

        if (!possuiDoc && !ignorarSimilaridade) {
            List<Paciente> similares = pacienteRepository
                    .findByNomeIgnoreCaseAndNomeMaeIgnoreCaseAndDataNascimento(
                            pacienteDTO.nome(),
                            pacienteDTO.nomeMae(),
                            pacienteDTO.dataNascimento()
                    );

            similares.removeIf(p -> Objects.equals(
                    p.getIdPublico(),
                    pacienteDTO.idPublico()
            ));

            if (!similares.isEmpty()) {
                List<PacienteDTO> similaresDTO = similares.stream()
                        .map(pacienteMapper::toPacienteDTO)
                        .toList();

                throw new DatabaseException(
                        "Registros similares encontrados. " +
                        "Confirme para prosseguir. " +
                        similaresDTO
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
            paciente.setCountFaltas(faltasConsecutivas(paciente) + 1);
        }
        if (novoStatus == SituacaoAtendimento.PRESENTE) {
            paciente.setCountFaltas(0);
            paciente.setDataUltimaPresenca(LocalDate.now());
            paciente.setGatilhoVisitaAcionado(false);
        }
        if (statusAnterior == SituacaoAtendimento.FALTOU && novoStatus != SituacaoAtendimento.FALTOU) {
            paciente.setCountFaltas(Math.max(0, faltasConsecutivas(paciente) - 1));
        }

        calcularEAtualizarRisco(paciente);
        if (paciente.getIdPublico() != null) {
            pacienteRepository.save(paciente);
        }
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
        int faltasConsecutivas = faltasConsecutivas(paciente);

        if (faltasConsecutivas >= limiteVermelhoFaltas || diasAusente > limiteVermelhoDias) {
            paciente.setClassificacaoRisco(ClassificacaoRisco.VERMELHO);
        }  
        // Depois o risco médio (Amarelo)
        else if (faltasConsecutivas >= limiteAmareloFaltas || diasAusente > limiteAmareloDias) {
            paciente.setClassificacaoRisco(ClassificacaoRisco.AMARELO);
        } 
        // Caso contrário, está tudo bem (Verde)
        else {
            paciente.setClassificacaoRisco(ClassificacaoRisco.VERDE);
        }
    }


    @Override
    @Transactional
    public void encerrarAcompanhamento(UUID idPublico, EncerramentoPacienteDTO encerramento) {
        Paciente paciente = pacienteRepository.findByIdPublicoForUpdate(idPublico)
                .orElseThrow(() -> new ResourceNotFoundException(idPublico));

        if (encerramento.motivo() == MotivoEncerramento.OUTRO
                && (encerramento.descricao() == null || encerramento.descricao().isBlank())) {
            throw new ValidationException("Descrição obrigatória quando o motivo do encerramento for OUTRO.");
        }else if(paciente.getStatusPaciente() == encerramento.statusPaciente()){
            throw new ValidationException("Paciente para encerramento não deve ter o mesmo status ou status ATIVO.");
        }

        paciente.setStatusPaciente(encerramento.statusPaciente());
        paciente.setMotivoEncerramento(encerramento.motivo());
        paciente.setDescricaoMotivoEncerramento(encerramento.descricao());
        paciente.setDataEncerramento(LocalDate.now());
        paciente.setProfissionalEncerramento(usuarioAuditoria());
        paciente.setClassificacaoRisco(ClassificacaoRisco.VERDE);
        paciente.setGatilhoVisitaAcionado(false);
        pacienteRepository.save(paciente);
        historicoPacienteService.registrarSituacaoAtual(paciente, "Acompanhamento encerrado. Situação atual: " + paciente.getStatusPaciente() + ".");
    }

    @Override
    @Transactional
    public void reativarAcompanhamento(UUID idPublico, ReativacaoPacienteDTO reativacao) {
        Paciente paciente = pacienteRepository.findByIdPublicoForUpdate(idPublico)
                .orElseThrow(() -> new ResourceNotFoundException(idPublico));

        paciente.setStatusPaciente(StatusPaciente.ATIVO);
        paciente.setDataReativacao(LocalDate.now());
        paciente.setMotivoReativacao(reativacao.motivo());
        paciente.setProfissionalReativacao(usuarioAuditoria());
        calcularEAtualizarRisco(paciente);
        pacienteRepository.save(paciente);
        historicoPacienteService.registrarSituacaoAtual(paciente, "Acompanhamento reativado. Situação atual: ATIVO.");
    }

    @Override
    public List<AlertaBuscaAtivaDTO> listarPacientesEmBuscaAtiva() {
        return pacienteRepository
                .findByStatusPacienteAndClassificacaoRisco(StatusPaciente.ATIVO, ClassificacaoRisco.VERMELHO)
                .stream()
                .map(this::montarAlertaBuscaAtiva)
                .toList();
    }

    private AlertaBuscaAtivaDTO montarAlertaBuscaAtiva(Paciente paciente) {
        boolean alertaDeVisita = Boolean.TRUE.equals(paciente.getGatilhoVisitaAcionado())
                || faltasConsecutivas(paciente) >= 2;

        String alerta = null;
        String localBusca = null;
        String avisoAdicional = null;

        if (alertaDeVisita) {
            if (paciente.isSituacaoRua()) {
                alerta = "Busca em ponto de referência necessária";
                localBusca = formatarPontoReferencia(paciente.getEndereco());
                if (localBusca == null) {
                    avisoAdicional = "Nenhum ponto de referência cadastrado — verificar com a equipe antes de acionar a busca.";
                }
            } else {
                alerta = "Visita domiciliar necessária";
                localBusca = formatarEndereco(paciente.getEndereco());
            }
        }

        return new AlertaBuscaAtivaDTO(
                paciente.getIdPublico(),
                paciente.getNome(),
                paciente.getClassificacaoRisco(),
                faltasConsecutivas(paciente),
                alerta,
                localBusca,
                avisoAdicional);
    }

    private int faltasConsecutivas(Paciente paciente) {
        return paciente.getCountFaltas();
    }

    private String formatarPontoReferencia(Endereco endereco) {
        if (endereco == null || endereco.getComplemento() == null || endereco.getComplemento().isBlank()) {
            return null;
        }
        return endereco.getComplemento();
    }

    private String formatarEndereco(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return String.join(", ", List.of(
                valor(endereco.getLogradouro()),
                valor(endereco.getNumero()),
                valor(endereco.getBairro()),
                valor(endereco.getCidade()),
                valor(endereco.getEstado())))
                .replaceAll("(, )+", ", ")
                .replaceAll("^, |, $", "");
    }

    private String valor(String valor) {
        return valor == null ? "" : valor;
    }

    private String usuarioAuditoria() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
    
}
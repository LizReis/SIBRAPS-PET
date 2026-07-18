package com.pet.buscaativa.config;

import com.pet.buscaativa.entities.Paciente;
import com.pet.buscaativa.entities.enums.StatusPaciente;
import com.pet.buscaativa.repositories.PacienteRepository;
import com.pet.buscaativa.services.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MonitoramentoBuscaAtiva {

    private final PacienteRepository pacienteRepository;
    private final PacienteService pacienteService;

    //Verificara todo dia à 1h da manhã (segundos, minutos, horas, dia, mes, ano)
    @Scheduled(cron = "0 0 1 * * ?") 
    @Transactional
    public void atualizarRiscosDiariamente() {
        // Busca todos os ativos
        List<Paciente> pacientesAtivos = pacienteRepository.findByStatusPaciente(StatusPaciente.ATIVO);
        
        for (Paciente p : pacientesAtivos) {
            // Se a data passar da meia-noite e bater os 120 dias, este método muda a cor para VERMELHO
            pacienteService.calcularEAtualizarRisco(p);
        }
        
        pacienteRepository.saveAll(pacientesAtivos);
    }
}

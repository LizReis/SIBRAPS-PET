import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { PacientePayload, PacienteService } from '../../services/paciente/paciente-service';
import {
  ProfissionalPayload,
  ProfissionalService,
} from '../../services/profissional/profissional-service';
import {
  AgendamentoService,
  NovoAgendamentoPayload,
  StandardError,
  ValidationError,
  VagasPorTurno,
} from '../../services/agendamento-service';

@Component({
  selector: 'app-novo-agendamento',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './novo-agendamento.html',
  styleUrl: './novo-agendamento.css',
})
export class NovoAgendamento implements OnInit {
  // Passo 1: paciente
  termoPesquisaPaciente = '';
  resultadosPacientes: PacientePayload[] = [];
  pacienteSelecionado: PacientePayload | null = null;
  buscandoPaciente = false;

  // Passo 2: profissional
  profissionais: ProfissionalPayload[] = [];
  profissionalSelecionadoId: string | null = null;

  // Passo 3: data e turno
  dataSelecionada = '';
  vagas: VagasPorTurno | null = null;
  consultandoVagas = false;
  turnoSelecionado: string | null = null;

  // Passo 4: horário
  horaAtendimento = '';

  salvando = false;
  erroGeral: string | null = null;
  errosPorCampo: Record<string, string> = {};

  constructor(
    private router: Router,
    private pacienteService: PacienteService,
    private profissionalService: ProfissionalService,
    private agendamentoService: AgendamentoService,
  ) {}

  ngOnInit(): void {
    this.carregarProfissionais();
  }

  carregarProfissionais(): void {
    this.profissionalService.listar().subscribe({
      next: (profissionais) => {
        // Recepção não atende, então não entra como opção de profissional aqui
        this.profissionais = profissionais.filter((p) => p.tipoUsuario !== 'RECEPCAO');
      },
      error: (erro) => {
        console.error('Erro ao carregar profissionais', erro);
        this.erroGeral = 'Não foi possível carregar a lista de profissionais.';
      },
    });
  }

  pesquisarPaciente(): void {
    const termo = this.termoPesquisaPaciente.trim();

    if (termo.length < 2) {
      this.resultadosPacientes = [];
      return;
    }

    this.buscandoPaciente = true;

    this.pacienteService.buscarPorNome(termo).subscribe({
      next: (pacientes) => {
        this.resultadosPacientes = pacientes;
        this.buscandoPaciente = false;
      },
      error: () => {
        this.resultadosPacientes = [];
        this.buscandoPaciente = false;
      },
    });
  }

  selecionarPaciente(paciente: PacientePayload): void {
    this.pacienteSelecionado = paciente;
    this.resultadosPacientes = [];
    this.termoPesquisaPaciente = '';
  }

  trocarPaciente(): void {
    this.pacienteSelecionado = null;
  }

  onProfissionalOuDataAlterado(): void {
    this.vagas = null;
    this.turnoSelecionado = null;
    this.erroGeral = null;

    if (!this.profissionalSelecionadoId || !this.dataSelecionada) {
      return;
    }

    this.consultandoVagas = true;

    this.agendamentoService
      .consultarVagas(this.profissionalSelecionadoId, this.dataSelecionada)
      .subscribe({
        next: (vagas) => {
          this.vagas = vagas;
          this.consultandoVagas = false;
        },
        error: (erro) => {
          console.error('Erro ao consultar vagas', erro);
          this.erroGeral = 'Não foi possível consultar as vagas disponíveis.';
          this.consultandoVagas = false;
        },
      });
  }

  selecionarTurno(turno: string): void {
    if (this.vagasDoTurno(turno) <= 0) return;
    this.turnoSelecionado = turno;
  }

  vagasDoTurno(turno: string): number {
    if (!this.vagas) return 0;
    return turno === 'MANHA' ? this.vagas.MANHA : this.vagas.TARDE;
  }

  get podeConfirmar(): boolean {
    return !!(
      this.pacienteSelecionado &&
      this.profissionalSelecionadoId &&
      this.dataSelecionada &&
      this.turnoSelecionado &&
      this.horaAtendimento &&
      !this.salvando
    );
  }

  confirmarAgendamento(): void {
    if (!this.podeConfirmar) return;

    this.erroGeral = null;
    this.errosPorCampo = {};
    this.salvando = true;

    const payload: NovoAgendamentoPayload = {
      usuarioId: this.profissionalSelecionadoId!,
      pacienteId: this.pacienteSelecionado!.idPublico!,
      dataAgendamento: this.dataSelecionada,
      turnoAgendamento: this.turnoSelecionado!,
      horaAtendimento: this.horaAtendimento,
    };

    this.agendamentoService.criarAgendamento(payload).subscribe({
      next: () => {
        this.salvando = false;
        this.router.navigate(['/agenda'], {
          queryParams: { data: this.dataSelecionada },
        });
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando = false;
        this.tratarErro(erro);
      },
    });
  }

  private tratarErro(erro: HttpErrorResponse): void {
    if (!erro.error) {
      this.erroGeral = 'Não foi possível conectar ao servidor. Tente novamente.';
      return;
    }

    if (erro.status === 422 && Array.isArray(erro.error.errors)) {
      const validationError = erro.error as ValidationError;
      validationError.errors.forEach((campo) => {
        this.errosPorCampo[campo.fieldName] = campo.message;
      });
      this.erroGeral = validationError.message;
      return;
    }

    if (erro.error.message) {
      const standardError = erro.error as StandardError;
      this.erroGeral = standardError.message;
      return;
    }

    this.erroGeral = 'Não foi possível criar o agendamento.';
  }

  cancelar(): void {
    this.router.navigate(['/agenda']);
  }

  labelEnum(valor: string | undefined): string {
    if (!valor) return '-';
    return valor
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }

  formatarCpf(cpf: string | undefined): string {
    if (!cpf) return '-';
    const numeros = cpf.replace(/\D/g, '');
    return numeros.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  }
}

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  ProfissionalPayload,
  ProfissionalService,
} from '../../services/profissional/profissional-service';
import {
  AgendamentoDTO,
  AgendamentoService,
  StandardError,
} from '../../services/agendamento-service';
import { UsuarioLogadoService } from '../../services/usuario-logado-service';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './agenda.html',
  styleUrl: './agenda.css',
})
export class Agenda implements OnInit {
  dataSelecionada: string = this.formatarDataISO(new Date());

  podeFiltrarProfissional = false;
  podeConfigurarAgenda = false;
  podeCriarAgendamento = false;
  carregandoUsuarioLogado = true;

  profissionais: ProfissionalPayload[] = [];
  profissionalSelecionadoId: string | null = null;

  agendamentos: AgendamentoDTO[] = [];

  carregando = false;
  erroGeral: string | null = null;

  atualizandoId: number | null = null;

  constructor(
    private usuarioLogadoService: UsuarioLogadoService,
    private profissionalService: ProfissionalService,
    private agendamentoService: AgendamentoService,
  ) {}

  ngOnInit(): void {
    this.usuarioLogadoService.obterUsuarioLogado().subscribe({
      next: (usuario) => {
        this.podeFiltrarProfissional = usuario.tipoUsuario !== 'PROFISSIONAL';
        this.podeConfigurarAgenda =
          usuario.tipoUsuario === 'ADMINISTRADOR' || usuario.tipoUsuario === 'PROFISSIONAL';
        this.podeCriarAgendamento =
          usuario.tipoUsuario === 'ADMINISTRADOR' || usuario.tipoUsuario === 'RECEPCAO';
        this.carregandoUsuarioLogado = false;

        if (this.podeFiltrarProfissional) {
          this.carregarProfissionais();
        }

        this.carregarAgenda();
      },
      error: (erro) => {
        console.error('Erro ao identificar usuário logado', erro);
        this.erroGeral = 'Não foi possível identificar o usuário logado.';
        this.carregandoUsuarioLogado = false;
      },
    });
  }

  carregarProfissionais(): void {
    this.profissionalService.listar().subscribe({
      next: (profissionais) => {
        this.profissionais = profissionais;
      },
      error: (erro) => {
        console.error('Erro ao carregar profissionais', erro);
      },
    });
  }

  carregarAgenda(): void {
    this.carregando = true;
    this.erroGeral = null;

    const profissionalId = this.podeFiltrarProfissional
      ? (this.profissionalSelecionadoId ?? undefined)
      : undefined;

    this.agendamentoService.buscarAgendaDoDia(this.dataSelecionada, profissionalId).subscribe({
      next: (agendamentos) => {
        this.agendamentos = agendamentos.sort((a, b) => {
          if (a.turnoAgendamento !== b.turnoAgendamento) {
            return a.turnoAgendamento === 'MANHA' ? -1 : 1;
          }
          return a.horaAtendimento.localeCompare(b.horaAtendimento);
        });
        this.carregando = false;
      },
      error: (erro) => {
        console.error('Erro ao carregar agenda', erro);
        this.erroGeral = 'Não foi possível carregar a agenda do dia.';
        this.carregando = false;
      },
    });
  }

  onDataAlterada(): void {
    this.carregarAgenda();
  }

  onProfissionalAlterado(): void {
    this.carregarAgenda();
  }

  irParaHoje(): void {
    this.dataSelecionada = this.formatarDataISO(new Date());
    this.carregarAgenda();
  }

  mudarDia(offsetDias: number): void {
    const data = new Date(this.dataSelecionada + 'T00:00:00');
    data.setDate(data.getDate() + offsetDias);
    this.dataSelecionada = this.formatarDataISO(data);
    this.carregarAgenda();
  }

  get agendamentosManha(): AgendamentoDTO[] {
    return this.agendamentos.filter((a) => a.turnoAgendamento === 'MANHA');
  }

  get agendamentosTarde(): AgendamentoDTO[] {
    return this.agendamentos.filter((a) => a.turnoAgendamento === 'TARDE');
  }

  get dataEhFutura(): boolean {
    return this.dataSelecionada > this.formatarDataISO(new Date());
  }

  podeRegistrarFrequencia(agendamento: AgendamentoDTO): boolean {
    if (this.dataEhFutura) return false;
    return (
      agendamento.situacaoAtendimento === 'AGENDADO' ||
      agendamento.situacaoAtendimento === 'REMARCADO'
    );
  }

  registrarPresenca(agendamento: AgendamentoDTO): void {
    this.marcarStatus(agendamento, 'PRESENTE');
  }

  registrarFalta(agendamento: AgendamentoDTO): void {
    this.marcarStatus(agendamento, 'FALTOU');
  }

  private marcarStatus(agendamento: AgendamentoDTO, novoStatus: string): void {
    this.atualizandoId = agendamento.id;
    this.erroGeral = null;

    this.agendamentoService
      .atualizarStatus(agendamento.id, novoStatus, agendamento.version)
      .subscribe({
        next: (atualizado) => {
          const index = this.agendamentos.findIndex((a) => a.id === atualizado.id);
          if (index !== -1) {
            this.agendamentos[index] = atualizado;
          }
          this.atualizandoId = null;
        },
        error: (erro: HttpErrorResponse) => {
          this.atualizandoId = null;

          if (erro.status === 409) {
            this.erroGeral =
              'Este agendamento foi alterado por outra pessoa. A agenda foi atualizada — confira antes de tentar novamente.';
            this.carregarAgenda();
            return;
          }

          const standardError = erro.error as StandardError | undefined;
          this.erroGeral =
            standardError?.message ?? 'Não foi possível atualizar o status do agendamento.';
        },
      });
  }

  labelSituacao(situacao: string): string {
    const labels: Record<string, string> = {
      AGENDADO: 'Agendado',
      PRESENTE: 'Presente',
      FALTOU: 'Faltou',
      REMARCADO: 'Remarcado',
      CANCELADO: 'Cancelado',
      REMARCADO_ORIGEM: 'Remarcado (origem)',
    };
    return labels[situacao] ?? situacao;
  }

  classeSituacao(situacao: string): string {
    return 'badge-' + situacao.toLowerCase().replace('_', '-');
  }

  labelTipoAcompanhamento(valor: string): string {
    return (
      valor
        ?.replaceAll('_', ' ')
        .toLowerCase()
        .replace(/\b\w/g, (letra) => letra.toUpperCase()) ?? '-'
    );
  }

  formatarHora(hora: string): string {
    return hora?.slice(0, 5) ?? '-';
  }

  private formatarDataISO(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}

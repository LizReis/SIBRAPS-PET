import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import {
  ProfissionalPayload,
  ProfissionalService,
} from '../../services/profissional/profissional-service';

import { BloqueioAgendaDTO, BloqueioAgendaService } from '../../services/bloqueio-agenda-service';
import { UsuarioLogadoService } from '../../services/usuario-logado-service';
import {
  DisponibilidadeDTO,
  DisponibilidadeService,
  StandardError,
} from '../../services/disponibilidade-service';

interface DiaSemanaOption {
  valor: string;
  label: string;
}

@Component({
  selector: 'app-configuracao-agenda',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracao-agenda.html',
  styleUrl: './configuracao-agenda.css',
})
export class ConfiguracaoAgenda implements OnInit {
  diasSemana: DiaSemanaOption[] = [
    { valor: 'MONDAY', label: 'Segunda-feira' },
    { valor: 'TUESDAY', label: 'Terça-feira' },
    { valor: 'WEDNESDAY', label: 'Quarta-feira' },
    { valor: 'THURSDAY', label: 'Quinta-feira' },
    { valor: 'FRIDAY', label: 'Sexta-feira' },
    { valor: 'SATURDAY', label: 'Sábado' },
    { valor: 'SUNDAY', label: 'Domingo' },
  ];

  turnos = ['MANHA', 'TARDE'];

  isAdmin = false;
  carregandoUsuarioLogado = true;

  profissionais: ProfissionalPayload[] = [];
  profissionalSelecionadoId: string | null = null;

  disponibilidades: DisponibilidadeDTO[] = [];
  bloqueios: BloqueioAgendaDTO[] = [];

  carregandoDados = false;
  erroGeral: string | null = null;

  salvandoDisponibilidade = false;
  salvandoBloqueio = false;

  novaDisponibilidade: DisponibilidadeDTO = {
    diaSemana: '',
    turno: '',
    capacidade: 1,
  };

  novoBloqueio: BloqueioAgendaDTO = {
    dataInicio: '',
    dataFim: '',
    motivoBloqueio: '',
  };

  constructor(
    private usuarioLogadoService: UsuarioLogadoService,
    private profissionalService: ProfissionalService,
    private disponibilidadeService: DisponibilidadeService,
    private bloqueioAgendaService: BloqueioAgendaService,
  ) {}

  ngOnInit(): void {
    this.usuarioLogadoService.obterUsuarioLogado().subscribe({
      next: (usuario) => {
        this.isAdmin = usuario.tipoUsuario === 'ADMINISTRADOR';
        this.carregandoUsuarioLogado = false;

        if (this.isAdmin) {
          this.carregarProfissionais();
        } else {
          this.carregarDados();
        }
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
        this.erroGeral = 'Não foi possível carregar a lista de profissionais.';
      },
    });
  }

  onProfissionalSelecionado(): void {
    this.disponibilidades = [];
    this.bloqueios = [];

    if (this.profissionalSelecionadoId) {
      this.carregarDados(this.profissionalSelecionadoId);
    }
  }

  carregarDados(usuarioId?: string): void {
    this.carregandoDados = true;
    this.erroGeral = null;

    this.disponibilidadeService.listar(usuarioId).subscribe({
      next: (disponibilidades) => {
        this.disponibilidades = disponibilidades;
        this.carregandoDados = false;
      },
      error: (erro) => {
        console.error('Erro ao carregar disponibilidades', erro);
        this.erroGeral = 'Não foi possível carregar as disponibilidades.';
        this.carregandoDados = false;
      },
    });

    this.bloqueioAgendaService.listar(usuarioId).subscribe({
      next: (bloqueios) => {
        this.bloqueios = bloqueios;
      },
      error: (erro) => {
        console.error('Erro ao carregar bloqueios', erro);
        this.erroGeral = 'Não foi possível carregar os bloqueios de agenda.';
      },
    });
  }

  get usuarioIdAtivo(): string | undefined {
    // Admin: usa o profissional selecionado no dropdown.
    // Profissional: não envia nada — o backend resolve pelo token.
    return this.isAdmin ? (this.profissionalSelecionadoId ?? undefined) : undefined;
  }

  get podeGerenciar(): boolean {
    return !this.isAdmin || !!this.profissionalSelecionadoId;
  }

  adicionarDisponibilidade(): void {
    if (
      !this.novaDisponibilidade.diaSemana ||
      !this.novaDisponibilidade.turno ||
      !this.novaDisponibilidade.capacidade
    ) {
      this.erroGeral = 'Preencha dia da semana, turno e capacidade.';
      return;
    }

    this.salvandoDisponibilidade = true;
    this.erroGeral = null;

    const payload: DisponibilidadeDTO = {
      ...this.novaDisponibilidade,
      usuarioId: this.usuarioIdAtivo,
    };

    this.disponibilidadeService.salvar(payload).subscribe({
      next: () => {
        this.salvandoDisponibilidade = false;
        this.novaDisponibilidade = { diaSemana: '', turno: '', capacidade: 1 };
        this.carregarDados(this.usuarioIdAtivo);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvandoDisponibilidade = false;
        this.erroGeral = this.extrairMensagemErro(
          erro,
          'Não foi possível salvar a disponibilidade.',
        );
      },
    });
  }

  removerDisponibilidade(id: number | undefined): void {
    if (!id) return;

    this.disponibilidadeService.remover(id).subscribe({
      next: () => {
        this.disponibilidades = this.disponibilidades.filter((d) => d.id !== id);
      },
      error: (erro) => {
        console.error('Erro ao remover disponibilidade', erro);
        this.erroGeral = 'Não foi possível remover a disponibilidade.';
      },
    });
  }

  adicionarBloqueio(): void {
    if (!this.novoBloqueio.dataInicio || !this.novoBloqueio.dataFim) {
      this.erroGeral = 'Preencha a data de início e fim do bloqueio.';
      return;
    }

    if (this.novoBloqueio.dataFim < this.novoBloqueio.dataInicio) {
      this.erroGeral = 'A data final não pode ser anterior à data inicial.';
      return;
    }

    this.salvandoBloqueio = true;
    this.erroGeral = null;

    const payload: BloqueioAgendaDTO = {
      ...this.novoBloqueio,
      usuarioId: this.usuarioIdAtivo,
    };

    this.bloqueioAgendaService.salvar(payload).subscribe({
      next: () => {
        this.salvandoBloqueio = false;
        this.novoBloqueio = { dataInicio: '', dataFim: '', motivoBloqueio: '' };
        this.carregarDados(this.usuarioIdAtivo);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvandoBloqueio = false;
        this.erroGeral = this.extrairMensagemErro(erro, 'Não foi possível salvar o bloqueio.');
      },
    });
  }

  removerBloqueio(id: number | undefined): void {
    if (!id) return;

    this.bloqueioAgendaService.remover(id).subscribe({
      next: () => {
        this.bloqueios = this.bloqueios.filter((b) => b.id !== id);
      },
      error: (erro) => {
        console.error('Erro ao remover bloqueio', erro);
        this.erroGeral = 'Não foi possível remover o bloqueio.';
      },
    });
  }

  private extrairMensagemErro(erro: HttpErrorResponse, padrao: string): string {
    if (erro.error?.message) {
      return (erro.error as StandardError).message;
    }
    return padrao;
  }

  labelDiaSemana(valor: string): string {
    return this.diasSemana.find((d) => d.valor === valor)?.label ?? valor;
  }

  labelTurno(valor: string): string {
    return valor === 'MANHA' ? 'Manhã' : 'Tarde';
  }

  formatarData(data: string): string {
    if (!data) return '-';
    const [ano, mes, dia] = data.split('-');
    return `${dia}/${mes}/${ano}`;
  }
}

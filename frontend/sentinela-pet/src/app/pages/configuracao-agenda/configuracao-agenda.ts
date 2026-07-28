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
import {
  DisponibilidadeExcecaoDTO,
  DisponibilidadeExcecaoService,
} from '../../services/disponibilidade-excecao-service';

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
  excecoes: DisponibilidadeExcecaoDTO[] = [];

  carregandoDados = false;
  erroGeral: string | null = null;

  salvandoDisponibilidade = false;
  salvandoBloqueio = false;
  salvandoExcecao = false;

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

  novaExcecao: DisponibilidadeExcecaoDTO = {
    data: '',
    turno: '',
    capacidade: 1,
  };

  constructor(
    private usuarioLogadoService: UsuarioLogadoService,
    private profissionalService: ProfissionalService,
    private disponibilidadeService: DisponibilidadeService,
    private bloqueioAgendaService: BloqueioAgendaService,
    private disponibilidadeExcecaoService: DisponibilidadeExcecaoService,
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
    this.excecoes = [];

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

    this.disponibilidadeExcecaoService.listar(usuarioId).subscribe({
      next: (excecoes) => {
        this.excecoes = excecoes.sort((a, b) => a.data.localeCompare(b.data));
      },
      error: (erro) => {
        console.error('Erro ao carregar exceções', erro);
        this.erroGeral = 'Não foi possível carregar as datas configuradas.';
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

  adicionarExcecao(): void {
    if (!this.novaExcecao.data || !this.novaExcecao.turno || this.novaExcecao.capacidade == null) {
      this.erroGeral = 'Preencha data, turno e capacidade (pode ser 0 para fechar o turno).';
      return;
    }

    this.salvandoExcecao = true;
    this.erroGeral = null;

    const payload: DisponibilidadeExcecaoDTO = {
      ...this.novaExcecao,
      usuarioId: this.usuarioIdAtivo,
    };

    this.disponibilidadeExcecaoService.salvar(payload).subscribe({
      next: () => {
        this.salvandoExcecao = false;
        this.novaExcecao = { data: '', turno: '', capacidade: 1 };
        this.carregarDados(this.usuarioIdAtivo);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvandoExcecao = false;
        this.erroGeral = this.extrairMensagemErro(
          erro,
          'Não foi possível salvar a configuração da data.',
        );
      },
    });
  }

  removerExcecao(id: number | undefined): void {
    if (!id) return;

    this.disponibilidadeExcecaoService.remover(id).subscribe({
      next: () => {
        this.excecoes = this.excecoes.filter((e) => e.id !== id);
      },
      error: (erro) => {
        console.error('Erro ao remover exceção', erro);
        this.erroGeral = 'Não foi possível remover essa configuração.';
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

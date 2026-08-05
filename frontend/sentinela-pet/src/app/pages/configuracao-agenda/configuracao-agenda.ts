import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';

import {
  BloqueioAgendaDTO,
  BloqueioAgendaService,
} from '../../services/bloqueio-agenda-service';
import {
  DisponibilidadeExcecaoDTO,
  DisponibilidadeExcecaoService,
} from '../../services/disponibilidade-excecao-service';
import {
  DisponibilidadeDTO,
  DisponibilidadeService,
  StandardError,
} from '../../services/disponibilidade-service';
import {
  ProfissionalPayload,
  ProfissionalService,
} from '../../services/profissional/profissional-service';
import { UsuarioLogadoService } from '../../services/usuario-logado-service';

type Aba = 'horarios' | 'bloqueios' | 'excecoes';
type Modal = 'horario' | 'bloqueio' | 'excecao' | 'confirmacao' | null;

type AcaoExclusao = {
  tipo: Aba;
  id: number;
};

type DiaSemanaOption = {
  valor: string;
  label: string;
  curto: string;
};

@Component({
  selector: 'app-configuracao-agenda',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracao-agenda.html',
  styleUrl: './configuracao-agenda.css',
})
export class ConfiguracaoAgenda implements OnInit {
  readonly diasSemana: DiaSemanaOption[] = [
    { valor: 'MONDAY', label: 'Segunda-feira', curto: 'SEG' },
    { valor: 'TUESDAY', label: 'Terça-feira', curto: 'TER' },
    { valor: 'WEDNESDAY', label: 'Quarta-feira', curto: 'QUA' },
    { valor: 'THURSDAY', label: 'Quinta-feira', curto: 'QUI' },
    { valor: 'FRIDAY', label: 'Sexta-feira', curto: 'SEX' },
    { valor: 'SATURDAY', label: 'Sábado', curto: 'SÁB' },
    { valor: 'SUNDAY', label: 'Domingo', curto: 'DOM' },
  ];

  readonly turnos = ['MANHA', 'TARDE'];

  abaAtiva: Aba = 'horarios';
  modal: Modal = null;

  isAdmin = false;
  isProfissional = false;
  carregandoUsuarioLogado = true;
  carregandoDados = false;
  salvando = false;

  nomeUsuario = '';
  profissionais: ProfissionalPayload[] = [];
  profissionalSelecionadoId: string | null = null;

  disponibilidades: DisponibilidadeDTO[] = [];
  bloqueios: BloqueioAgendaDTO[] = [];
  excecoes: DisponibilidadeExcecaoDTO[] = [];

  erroGeral: string | null = null;
  sucesso: string | null = null;
  erroModal: string | null = null;

  disponibilidadeForm: DisponibilidadeDTO = {
    diaSemana: '',
    turno: '',
    capacidade: 1,
  };

  bloqueioForm: BloqueioAgendaDTO = {
    dataInicio: '',
    dataFim: '',
    motivoBloqueio: '',
  };

  excecaoForm: DisponibilidadeExcecaoDTO = {
    data: '',
    turno: '',
    capacidade: 1,
  };

  acaoExclusao: AcaoExclusao | null = null;
  elementoOrigem: HTMLElement | null = null;

  constructor(
    private readonly usuarioLogadoService: UsuarioLogadoService,
    private readonly profissionalService: ProfissionalService,
    private readonly disponibilidadeService: DisponibilidadeService,
    private readonly bloqueioAgendaService: BloqueioAgendaService,
    private readonly disponibilidadeExcecaoService: DisponibilidadeExcecaoService,
  ) {}

  ngOnInit(): void {
    this.usuarioLogadoService.obterUsuarioLogado().subscribe({
      next: (usuario) => {
        this.isAdmin = usuario.tipoUsuario === 'ADMINISTRADOR';
        this.isProfissional = usuario.tipoUsuario === 'PROFISSIONAL';
        this.nomeUsuario = usuario.nome;
        this.carregandoUsuarioLogado = false;

        if (this.isAdmin) {
          this.carregarProfissionais();
        } else if (this.isProfissional) {
          this.carregarDados();
        }
      },
      error: () => {
        this.carregandoUsuarioLogado = false;
        this.erroGeral = 'Não foi possível identificar o usuário logado.';
      },
    });
  }

  carregarProfissionais(): void {
    this.profissionalService.listar().subscribe({
      next: (profissionais) => {
        this.profissionais = profissionais.filter(
          (profissional) => profissional.tipoUsuario === 'PROFISSIONAL',
        );
      },
      error: () => {
        this.erroGeral = 'Não foi possível carregar a lista de profissionais.';
      },
    });
  }

  onProfissionalSelecionado(): void {
    this.limparDados();

    if (this.profissionalSelecionadoId) {
      this.carregarDados(this.profissionalSelecionadoId);
    }
  }

  limparDados(): void {
    this.disponibilidades = [];
    this.bloqueios = [];
    this.excecoes = [];
    this.erroGeral = null;
    this.sucesso = null;
  }

  carregarDados(usuarioId?: string): void {
    this.carregandoDados = true;
    this.erroGeral = null;

    forkJoin({
      disponibilidades: this.disponibilidadeService.listar(usuarioId),
      bloqueios: this.bloqueioAgendaService.listar(usuarioId),
      excecoes: this.disponibilidadeExcecaoService.listar(usuarioId),
    }).subscribe({
      next: (dados) => {
        this.disponibilidades = this.ordenarHorarios(dados.disponibilidades);
        this.bloqueios = dados.bloqueios;
        this.excecoes = [...dados.excecoes].sort((a, b) =>
          a.data.localeCompare(b.data),
        );
        this.carregandoDados = false;
      },
      error: (erro: HttpErrorResponse) => {
        this.carregandoDados = false;
        this.erroGeral = this.extrairMensagemErro(
          erro,
          'Não foi possível carregar a configuração da agenda.',
        );
      },
    });
  }

  get usuarioIdAtivo(): string | undefined {
    return this.isAdmin
      ? (this.profissionalSelecionadoId ?? undefined)
      : undefined;
  }

  get podeGerenciar(): boolean {
    const possuiProfissionalAlvo = this.isAdmin
      ? !!this.profissionalSelecionadoId
      : this.isProfissional;

    return possuiProfissionalAlvo && !this.carregandoDados;
  }

  selecionarAba(aba: Aba): void {
    this.abaAtiva = aba;
  }

  navegarAbas(event: KeyboardEvent): void {
    const abas: Aba[] = ['horarios', 'bloqueios', 'excecoes'];
    const indiceAtual = abas.indexOf(this.abaAtiva);

    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') {
      return;
    }

    event.preventDefault();

    const deslocamento = event.key === 'ArrowRight' ? 1 : -1;
    const proximoIndice = (indiceAtual + deslocamento + abas.length) % abas.length;

    this.abaAtiva = abas[proximoIndice];

    setTimeout(() => {
      document.getElementById(`tab-${this.abaAtiva}`)?.focus();
    });
  }

  abrirHorario(d?: DisponibilidadeDTO, origem?: Event): void {
    this.prepararModal(origem);
    this.disponibilidadeForm = d
      ? { ...d }
      : {
          diaSemana: '',
          turno: '',
          capacidade: 1,
        };
    this.modal = 'horario';
  }

  abrirBloqueio(b?: BloqueioAgendaDTO, origem?: Event): void {
    this.prepararModal(origem);
    this.bloqueioForm = b
      ? { ...b }
      : {
          dataInicio: '',
          dataFim: '',
          motivoBloqueio: '',
        };
    this.modal = 'bloqueio';
  }

  abrirExcecao(e?: DisponibilidadeExcecaoDTO, origem?: Event): void {
    this.prepararModal(origem);
    this.excecaoForm = e
      ? { ...e }
      : {
          data: '',
          turno: '',
          capacidade: 1,
        };
    this.modal = 'excecao';
  }

  confirmarExclusao(tipo: Aba, id: number | undefined, origem?: Event): void {
    if (id == null) {
      return;
    }

    this.prepararModal(origem);
    this.acaoExclusao = { tipo, id };
    this.modal = 'confirmacao';
  }

  prepararModal(origem?: Event): void {
    this.elementoOrigem = (origem?.currentTarget as HTMLElement | null) ?? null;
    this.erroModal = null;
    this.sucesso = null;

    setTimeout(() => {
      document
        .querySelector<HTMLElement>(
          '.modal-card input:not([readonly]), .modal-card select:not([disabled]), .modal-card button',
        )
        ?.focus();
    });
  }

  fecharModal(): void {
    if (this.salvando) {
      return;
    }

    this.modal = null;
    this.erroModal = null;
    this.acaoExclusao = null;

    setTimeout(() => this.elementoOrigem?.focus());
  }

  @HostListener('document:keydown.escape')
  aoEscape(): void {
    if (this.modal) {
      this.fecharModal();
    }
  }

  prenderFoco(event: KeyboardEvent): void {
    if (event.key !== 'Tab') {
      return;
    }

    const itens = Array.from(
      document.querySelectorAll<HTMLElement>(
        '.modal-card button:not([disabled]), .modal-card input:not([disabled]), .modal-card select:not([disabled])',
      ),
    );

    if (!itens.length) {
      return;
    }

    const primeiro = itens[0];
    const ultimo = itens[itens.length - 1];

    if (event.shiftKey && document.activeElement === primeiro) {
      event.preventDefault();
      ultimo.focus();
    } else if (!event.shiftKey && document.activeElement === ultimo) {
      event.preventDefault();
      primeiro.focus();
    }
  }

  salvarHorario(): void {
    const formulario = this.disponibilidadeForm;

    if (
      !formulario.diaSemana ||
      !formulario.turno ||
      !Number.isInteger(formulario.capacidade) ||
      formulario.capacidade < 1
    ) {
      this.erroModal =
        'Informe dia, turno e uma quantidade inteira de vagas maior que zero.';
      return;
    }

    const mensagem = formulario.id
      ? 'Horário atualizado com sucesso.'
      : 'Horário adicionado com sucesso.';

    this.executar(
      this.disponibilidadeService.salvar({
        ...formulario,
        usuarioId: this.usuarioIdAtivo,
      }),
      mensagem,
    );
  }

  salvarBloqueio(): void {
    const formulario = this.bloqueioForm;
    const motivo = formulario.motivoBloqueio?.trim();

    if (!formulario.dataInicio || !formulario.dataFim || !motivo) {
      this.erroModal = 'Preencha as datas e o motivo do bloqueio.';
      return;
    }

    if (formulario.dataFim < formulario.dataInicio) {
      this.erroModal = 'A data final não pode ser anterior à data inicial.';
      return;
    }

    const mensagem = formulario.id
      ? 'Bloqueio atualizado com sucesso.'
      : 'Bloqueio criado com sucesso.';

    this.executar(
      this.bloqueioAgendaService.salvar({
        ...formulario,
        motivoBloqueio: motivo,
        usuarioId: this.usuarioIdAtivo,
      }),
      mensagem,
    );
  }

  salvarExcecao(): void {
    const formulario = this.excecaoForm;

    if (
      !formulario.data ||
      !formulario.turno ||
      !Number.isInteger(formulario.capacidade) ||
      formulario.capacidade < 0
    ) {
      this.erroModal =
        'Informe data, turno e uma capacidade inteira igual ou maior que zero.';
      return;
    }

    const mensagem = formulario.id
      ? 'Exceção atualizada com sucesso.'
      : 'Exceção criada com sucesso.';

    this.executar(
      this.disponibilidadeExcecaoService.salvar({
        ...formulario,
        usuarioId: this.usuarioIdAtivo,
      }),
      mensagem,
    );
  }

  executar(observable: Observable<unknown>, mensagem: string): void {
    if (this.salvando) {
      return;
    }

    this.salvando = true;
    this.erroModal = null;

    observable.subscribe({
      next: () => {
        this.salvando = false;
        this.modal = null;
        this.acaoExclusao = null;
        this.sucesso = mensagem;
        this.carregarDados(this.usuarioIdAtivo);

        setTimeout(() => this.elementoOrigem?.focus());
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando = false;
        this.erroModal = this.extrairMensagemErro(
          erro,
          'Não foi possível concluir a operação.',
        );
      },
    });
  }

  excluir(): void {
    if (!this.acaoExclusao || this.salvando) {
      return;
    }

    const { tipo, id } = this.acaoExclusao;

    let operacao: Observable<unknown>;
    let mensagem: string;

    if (tipo === 'horarios') {
      operacao = this.disponibilidadeService.remover(id);
      mensagem = 'Horário excluído com sucesso.';
    } else if (tipo === 'bloqueios') {
      operacao = this.bloqueioAgendaService.remover(id);
      mensagem = 'Bloqueio excluído com sucesso.';
    } else {
      operacao = this.disponibilidadeExcecaoService.remover(id);
      mensagem = 'Exceção excluída com sucesso.';
    }

    this.executar(operacao, mensagem);
  }

  capacidade(dia: string, turno: string): number | null {
    return (
      this.disponibilidades.find(
        (disponibilidade) =>
          disponibilidade.diaSemana === dia && disponibilidade.turno === turno,
      )?.capacidade ?? null
    );
  }

  labelDiaSemana(valor: string): string {
    return this.diasSemana.find((dia) => dia.valor === valor)?.label ?? valor;
  }

  labelTurno(valor: string): string {
    return valor === 'MANHA' ? 'Manhã' : 'Tarde';
  }

  formatarData(data: string): string {
    if (!data) {
      return '-';
    }

    const [ano, mes, dia] = data.split('-');
    return `${dia}/${mes}/${ano}`;
  }

  periodo(bloqueio: BloqueioAgendaDTO): string {
    if (bloqueio.dataInicio === bloqueio.dataFim) {
      return this.formatarData(bloqueio.dataInicio);
    }

    return `${this.formatarData(bloqueio.dataInicio)} até ${this.formatarData(
      bloqueio.dataFim,
    )}`;
  }

  private ordenarHorarios(
    disponibilidades: DisponibilidadeDTO[],
  ): DisponibilidadeDTO[] {
    const ordemTurnos = new Map<string, number>([
      ['MANHA', 0],
      ['TARDE', 1],
    ]);

    return [...disponibilidades].sort((a, b) => {
      const diaA = this.diasSemana.findIndex((dia) => dia.valor === a.diaSemana);
      const diaB = this.diasSemana.findIndex((dia) => dia.valor === b.diaSemana);

      if (diaA !== diaB) {
        return diaA - diaB;
      }

      return (ordemTurnos.get(a.turno) ?? 99) - (ordemTurnos.get(b.turno) ?? 99);
    });
  }

  private extrairMensagemErro(
    erro: HttpErrorResponse,
    mensagemPadrao: string,
  ): string {
    return (erro.error as StandardError | undefined)?.message || mensagemPadrao;
  }
}

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ProfissionalPayload, ProfissionalService } from '../../services/profissional/profissional-service';
import { AgendamentoDTO, AgendamentoService, StandardError } from '../../services/agendamento-service';
import { UsuarioLogadoDTO, UsuarioLogadoService } from '../../services/usuario-logado-service';

type VisualizacaoAgenda = 'dia' | 'semana' | 'mes';

type DiaCalendario = { data: Date; iso: string; dia: number; foraDoMes: boolean; hoje: boolean; selecionado: boolean };
type DiaSemana = { iso: string; data: Date; titulo: string; subtitulo: string; agendamentos: AgendamentoDTO[] };
type DiaMes = DiaCalendario & { agendamentos: AgendamentoDTO[] };

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './agenda.html',
  styleUrl: './agenda.css',
})
export class Agenda implements OnInit, OnDestroy {
  private readonly horaInicial = 8;
  private readonly horaFinal = 18;
  private relogioId: ReturnType<typeof setInterval> | null = null;
  private ultimaConsulta = '';

  dataSelecionada: string = this.formatarDataISO(new Date());
  mesCalendario = this.inicioDoMes(this.criarDataLocal(this.dataSelecionada));
  visualizacao: VisualizacaoAgenda = 'dia';
  visualizacoes: { valor: VisualizacaoAgenda; label: string }[] = [
    { valor: 'dia', label: 'Dia' },
    { valor: 'semana', label: 'Semana' },
    { valor: 'mes', label: 'Mês' },
  ];

  usuarioLogado: UsuarioLogadoDTO | null = null;

  podeFiltrarProfissional = false;
  podeConfigurarAgenda = false;
  podeCriarAgendamento = false;
  carregandoUsuarioLogado = true;
  carregandoProfissionais = false;

  profissionais: ProfissionalPayload[] = [];
  profissionalSelecionadoId: string | null = null;

  agendamentos: AgendamentoDTO[] = [];

  carregando = false;
  erroGeral: string | null = null;

  atualizandoId: number | null = null;
  menuAbertoId: number | null = null;
  horarioAtual = new Date();

  readonly diasSemanaCurtos = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];
  readonly situacoesLegenda = ['PRESENTE', 'AGENDADO', 'FALTOU', 'REMARCADO', 'CANCELADO', 'REMARCADO_ORIGEM'];
  readonly statusClasses: Record<string, string> = {
    PRESENTE: 'status-presente', AGENDADO: 'status-agendado', FALTOU: 'status-faltou', REMARCADO: 'status-remarcado',
    CANCELADO: 'status-cancelado', REMARCADO_ORIGEM: 'status-remarcado-origem',
  };

  constructor(
    private usuarioLogadoService: UsuarioLogadoService,
    private profissionalService: ProfissionalService,
    private agendamentoService: AgendamentoService,
  ) {}

  ngOnInit(): void {
    this.iniciarRelogio();
    this.usuarioLogadoService.obterUsuarioLogado().subscribe({
      next: (usuario) => {
        this.usuarioLogado = usuario;
        this.podeFiltrarProfissional = usuario.tipoUsuario !== 'PROFISSIONAL';
        this.podeConfigurarAgenda = usuario.tipoUsuario === 'ADMINISTRADOR' || usuario.tipoUsuario === 'PROFISSIONAL';
        this.podeCriarAgendamento = usuario.tipoUsuario === 'ADMINISTRADOR' || usuario.tipoUsuario === 'RECEPCAO';
        this.carregandoUsuarioLogado = false;

        if (this.podeFiltrarProfissional) this.carregarProfissionais();
        this.carregarAgenda();
      },
      error: (erro) => { console.error('Erro ao identificar usuário logado', erro); this.erroGeral = 'Não foi possível identificar o usuário logado.'; this.carregandoUsuarioLogado = false; },
    });
  }

  ngOnDestroy(): void { if (this.relogioId) clearInterval(this.relogioId); }

  @HostListener('document:click') fecharMenuAoClicarFora(): void { this.menuAbertoId = null; }
  @HostListener('document:keydown.escape') fecharMenuComEscape(): void { this.menuAbertoId = null; }

  carregarProfissionais(): void {
    this.carregandoProfissionais = true;
    this.profissionalService.listar().subscribe({
      next: (profissionais) => { this.profissionais = profissionais.filter((p) => p.tipoUsuario === 'PROFISSIONAL'); this.carregandoProfissionais = false; },
      error: (erro) => { console.error('Erro ao carregar profissionais', erro); this.carregandoProfissionais = false; },
    });
  }

  carregarAgenda(forcar = false): void {
    const { inicio, fim } = this.periodoAtual();
    const profissionalId = this.podeFiltrarProfissional ? (this.profissionalSelecionadoId ?? undefined) : undefined;
    const chave = `${this.visualizacao}|${inicio}|${fim}|${profissionalId ?? 'todos'}`;
    if (!forcar && chave === this.ultimaConsulta) return;
    this.ultimaConsulta = chave;
    this.carregando = true; this.erroGeral = null; this.menuAbertoId = null;
    this.agendamentoService.buscarAgendaPorPeriodo(inicio, fim, profissionalId).subscribe({
      next: (agendamentos) => { this.agendamentos = this.ordenar(agendamentos); this.carregando = false; },
      error: (erro) => { console.error('Erro ao carregar agenda', erro); this.erroGeral = this.mensagemErroAgenda(); this.carregando = false; },
    });
  }

  
  alterarVisualizacao(v: VisualizacaoAgenda): void { this.visualizacao = v; this.mesCalendario = this.inicioDoMes(this.criarDataLocal(this.dataSelecionada)); this.carregarAgenda(); }
  onProfissionalAlterado(): void { this.carregarAgenda(true); }
  tentarNovamente(): void { this.carregarAgenda(true); }
  irParaHoje(): void { this.dataSelecionada = this.formatarDataISO(new Date()); this.mesCalendario = this.inicioDoMes(new Date()); this.carregarAgenda(true); }

  navegarPeriodo(offset: number): void {
    const data = this.criarDataLocal(this.dataSelecionada);
    if (this.visualizacao === 'dia') data.setDate(data.getDate() + offset);
    if (this.visualizacao === 'semana') data.setDate(data.getDate() + offset * 7);
    if (this.visualizacao === 'mes') data.setMonth(data.getMonth() + offset, 1);
    this.dataSelecionada = this.formatarDataISO(data); this.mesCalendario = this.inicioDoMes(data); this.carregarAgenda(true);
  }

  selecionarData(iso: string): void { this.dataSelecionada = iso; this.mesCalendario = this.inicioDoMes(this.criarDataLocal(iso)); this.carregarAgenda(true); }
  selecionarDiaDoMes(iso: string): void { this.dataSelecionada = iso; this.visualizacao = 'dia'; this.mesCalendario = this.inicioDoMes(this.criarDataLocal(iso)); this.carregarAgenda(true); }
  mudarMesCalendario(offset: number): void { const d = new Date(this.mesCalendario); d.setMonth(d.getMonth() + offset, 1); this.mesCalendario = d; }

  get horariosTimeline(): string[] { return Array.from({ length: this.horaFinal - this.horaInicial + 1 }, (_, i) => `${String(this.horaInicial + i).padStart(2, '0')}:00`); }
  agendamentosPorHora(hora: string): AgendamentoDTO[] { return this.agendamentos.filter((a) => this.formatarHora(a.horaAtendimento) === hora); }
  get mostrarIndicadorAtual(): boolean { return this.visualizacao === 'dia' && this.dataSelecionada === this.formatarDataISO(new Date()) && this.horarioAtual.getHours() >= this.horaInicial && this.horarioAtual.getHours() <= this.horaFinal; }
  get posicaoIndicadorAtual(): number { return ((this.horarioAtual.getHours() - this.horaInicial) * 60 + this.horarioAtual.getMinutes()) / ((this.horaFinal - this.horaInicial) * 60) * 100; }
  get periodoTitulo(): string { const { inicio, fim } = this.periodoAtual(); if (this.visualizacao === 'dia') return this.formatarDataLonga(this.criarDataLocal(inicio)); if (this.visualizacao === 'semana') return `${this.formatarDataCurta(this.criarDataLocal(inicio))} a ${this.formatarDataCurta(this.criarDataLocal(fim))}`; return this.formatarMesAno(this.criarDataLocal(inicio)); }
  get contagemTitulo(): string { const n = this.agendamentos.length; return `${n} atendimento${n === 1 ? '' : 's'} agendado${n === 1 ? '' : 's'}`; }
  get resumoTitulo(): string { return this.visualizacao === 'dia' ? 'Resumo do dia' : this.visualizacao === 'semana' ? 'Resumo da semana' : 'Resumo do mês'; }
  get mensagemVazia(): string { return this.visualizacao === 'dia' ? 'Nenhum atendimento agendado para esta data.' : this.visualizacao === 'semana' ? 'Nenhum atendimento encontrado nesta semana.' : 'Nenhum atendimento encontrado neste mês.'; }
  get calendario(): DiaCalendario[] { return this.montarGradeMensal(this.mesCalendario); }
  get diasDaSemana(): DiaSemana[] { const inicio = this.inicioDaSemana(this.criarDataLocal(this.dataSelecionada)); return Array.from({ length: 7 }, (_, i) => { const d = new Date(inicio); d.setDate(d.getDate() + i); const iso = this.formatarDataISO(d); return { iso, data: d, titulo: d.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', ''), subtitulo: d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }), agendamentos: this.agendamentos.filter((a) => a.dataAgendamento === iso) }; }); }
  get gradeMes(): DiaMes[] { return this.montarGradeMensal(this.criarDataLocal(this.dataSelecionada)).map((d) => ({ ...d, agendamentos: this.agendamentos.filter((a) => a.dataAgendamento === d.iso) })); }
  resumoSituacoes(): { situacao: string; total: number }[] { const c = new Map<string, number>(); this.agendamentos.forEach((a) => c.set(a.situacaoAtendimento, (c.get(a.situacaoAtendimento) ?? 0) + 1)); return Array.from(c.entries()).map(([situacao, total]) => ({ situacao, total })); }

  podeRegistrarFrequencia(a: AgendamentoDTO): boolean { return a.dataAgendamento <= this.formatarDataISO(new Date()) && ['AGENDADO', 'REMARCADO'].includes(a.situacaoAtendimento); }
  toggleMenu(event: Event, id: number): void { event.stopPropagation(); this.menuAbertoId = this.menuAbertoId === id ? null : id; }
  registrarPresenca(a: AgendamentoDTO): void { this.marcarStatus(a, 'PRESENTE'); }
  registrarFalta(a: AgendamentoDTO): void { this.marcarStatus(a, 'FALTOU'); }

  private marcarStatus(a: AgendamentoDTO, novoStatus: string): void { this.atualizandoId = a.id; this.erroGeral = null; this.menuAbertoId = null; this.agendamentoService.atualizarStatus(a.id, novoStatus, a.version).subscribe({ next: (atualizado) => { const i = this.agendamentos.findIndex((x) => x.id === atualizado.id); if (i !== -1) this.agendamentos[i] = atualizado; this.atualizandoId = null; }, error: (erro: HttpErrorResponse) => { this.atualizandoId = null; if (erro.status === 409) { this.erroGeral = 'Este agendamento foi alterado por outra pessoa. A agenda foi atualizada — confira antes de tentar novamente.'; this.carregarAgenda(true); return; } const standardError = erro.error as StandardError | undefined; this.erroGeral = standardError?.message ?? 'Não foi possível atualizar o status do agendamento.'; } }); }

  classeSituacao(s: string): string { return this.statusClasses[s] ?? 'status-neutro'; }
  labelSituacao(s: string): string { return s; }
  labelTipoAcompanhamento(v: string): string { return v?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (l) => l.toUpperCase()) ?? '-'; }
  formatarHora(h: string): string { return h?.slice(0, 5) ?? '-'; }
  private ordenar(lista: AgendamentoDTO[]): AgendamentoDTO[] { return [...lista].sort((a, b) => a.dataAgendamento.localeCompare(b.dataAgendamento) || a.turnoAgendamento.localeCompare(b.turnoAgendamento) || a.horaAtendimento.localeCompare(b.horaAtendimento) || a.id - b.id); }
  private periodoAtual(): { inicio: string; fim: string } { const d = this.criarDataLocal(this.dataSelecionada); if (this.visualizacao === 'semana') { const i = this.inicioDaSemana(d); const f = new Date(i); f.setDate(f.getDate() + 6); return { inicio: this.formatarDataISO(i), fim: this.formatarDataISO(f) }; } if (this.visualizacao === 'mes') { const i = this.inicioDoMes(d); const f = new Date(i.getFullYear(), i.getMonth() + 1, 0); return { inicio: this.formatarDataISO(i), fim: this.formatarDataISO(f) }; } return { inicio: this.dataSelecionada, fim: this.dataSelecionada }; }
  private montarGradeMensal(base: Date): DiaCalendario[] { const inicioMes = this.inicioDoMes(base); const cursor = this.inicioDaSemana(inicioMes); const hoje = this.formatarDataISO(new Date()); return Array.from({ length: 42 }, () => { const d = new Date(cursor); const iso = this.formatarDataISO(d); cursor.setDate(cursor.getDate() + 1); return { data: d, iso, dia: d.getDate(), foraDoMes: d.getMonth() !== inicioMes.getMonth(), hoje: iso === hoje, selecionado: iso === this.dataSelecionada }; }); }
  private inicioDaSemana(d: Date): Date { const r = new Date(d); const day = r.getDay(); const diff = day === 0 ? -6 : 1 - day; r.setDate(r.getDate() + diff); return this.semHora(r); }
  private inicioDoMes(d: Date): Date { return new Date(d.getFullYear(), d.getMonth(), 1); }
  private criarDataLocal(iso: string): Date { const [a, m, d] = iso.split('-').map(Number); return new Date(a, m - 1, d); }
  private semHora(d: Date): Date { return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }
  private formatarDataISO(data: Date): string { return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`; }
  private formatarDataLonga(d: Date): string { return d.toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }); }
  private formatarDataCurta(d: Date): string { return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }); }
  formatarMesAno(d: Date): string { return d.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' }); }
  private iniciarRelogio(): void { this.horarioAtual = new Date(); this.relogioId = setInterval(() => this.horarioAtual = new Date(), 60000); }
  private mensagemErroAgenda(): string { return this.visualizacao === 'dia' ? 'Não foi possível carregar a agenda do dia.' : this.visualizacao === 'semana' ? 'Não foi possível carregar a agenda da semana.' : 'Não foi possível carregar a agenda do mês.'; }
}

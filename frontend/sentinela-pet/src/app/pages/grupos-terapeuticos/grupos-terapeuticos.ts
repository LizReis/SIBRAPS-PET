import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import {
  GrupoTerapeuticoService,
  SessaoGrupoPayload,
  StatusVisualSessao,
} from "../../services/grupo-terapeutico-service";

@Component({
  selector: "app-grupos-terapeuticos",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: "./grupos-terapeuticos.html",
  styleUrl: "./grupos-terapeuticos.css",
})
export class GruposTerapeuticos implements OnInit {
  dataSelecionada = this.hoje();
  sessoes: SessaoGrupoPayload[] = [];
  proximasSessoes: SessaoGrupoPayload[] = [];
  filtroStatus = "";
  filtroCoordenador = "";
  buscaTema = "";
  carregando = false;
  erro = "";
  sessaoAberta: SessaoGrupoPayload | null = null;
  acaoEmAndamento = false;
  erroModal = "";

  constructor(
    private readonly service: GrupoTerapeuticoService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const data = this.route.snapshot.queryParamMap.get("data");
    if (data && /^\d{4}-\d{2}-\d{2}$/.test(data)) this.dataSelecionada = data;
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = "";
    const inicioFuturo = this.somarDias(this.hoje(), 1);
    const fimFuturo = this.somarDias(this.hoje(), 60);
    forkJoin({
      dia: this.service.listarSessoes(
        this.dataSelecionada,
        this.dataSelecionada
      ),
      futuras: this.service.listarSessoes(inicioFuturo, fimFuturo),
    }).subscribe({
      next: ({ dia, futuras }) => {
        this.sessoes = dia;
        this.proximasSessoes = futuras
          .filter((s) => s.status !== "CANCELADA")
          .sort((a, b) =>
            `${a.dataSessao}T${a.horario}`.localeCompare(
              `${b.dataSessao}T${b.horario}`
            )
          )
          .slice(0, 3);
        this.carregando = false;
      },
      error: () => {
        this.erro = "Não foi possível carregar as sessões. Tente novamente.";
        this.carregando = false;
      },
    });
  }

  get sessoesFiltradas(): SessaoGrupoPayload[] {
    const termo = this.buscaTema.trim().toLocaleLowerCase("pt-BR");
    return this.sessoes.filter(
      (s) =>
        (!this.filtroStatus || this.statusVisual(s) === this.filtroStatus) &&
        (!this.filtroCoordenador ||
          s.nomeCoordenador === this.filtroCoordenador) &&
        (!termo || s.temaGrupo.toLocaleLowerCase("pt-BR").includes(termo))
    );
  }
  get coordenadores(): string[] {
    return [...new Set(this.sessoes.map((s) => s.nomeCoordenador))].sort();
  }
  get totalAgendadas(): number {
    return this.sessoes.filter((s) => this.statusVisual(s) === "AGENDADA")
      .length;
  }
  get totalAndamento(): number {
    return this.sessoes.filter((s) => this.statusVisual(s) === "EM_ANDAMENTO")
      .length;
  }
  get totalRealizadas(): number {
    return this.sessoes.filter((s) => s.status === "REALIZADA").length;
  }
  get dataEhHoje(): boolean {
    return this.dataSelecionada === this.hoje();
  }

  statusVisual(sessao: SessaoGrupoPayload): StatusVisualSessao {
    if (
      sessao.status === "AGENDADA" &&
      sessao.dataSessao === this.hoje() &&
      sessao.horario.slice(0, 5) <= this.horaAtual()
    )
      return "EM_ANDAMENTO";
    return sessao.status;
  }
  labelStatus(sessao: SessaoGrupoPayload): string {
    return {
      AGENDADA: "Agendada",
      EM_ANDAMENTO: "Em andamento",
      REALIZADA: "Realizada",
      CANCELADA: "Cancelada",
    }[this.statusVisual(sessao)];
  }
  classeStatus(sessao: SessaoGrupoPayload): string {
    return this.statusVisual(sessao).toLowerCase();
  }
  mudarDia(dias: number): void {
    this.dataSelecionada = this.somarDias(this.dataSelecionada, dias);
    this.carregar();
  }
  irParaHoje(): void {
    this.dataSelecionada = this.hoje();
    this.carregar();
  }
  formatarData(data: string, completa = false): string {
    return new Intl.DateTimeFormat(
      "pt-BR",
      completa
        ? { weekday: "long", day: "2-digit", month: "long" }
        : { day: "2-digit", month: "short" }
    ).format(this.dataLocal(data));
  }
  abrirSessao(sessao: SessaoGrupoPayload): void {
    this.sessaoAberta = sessao;
    this.erroModal = "";
  }
  fecharSessao(): void {
    this.sessaoAberta = null;
  }
  removerParticipante(pacienteId: string): void {
    if (!this.sessaoAberta) return;
    this.acaoEmAndamento = true;
    this.erroModal = "";
    this.service
      .removerParticipante(this.sessaoAberta.id, pacienteId)
      .subscribe({
        next: (atualizada) => {
          this.atualizarSessao(atualizada);
          this.acaoEmAndamento = false;
        },
        error: (e) => {
          this.erroModal =
            e.error?.message || "Não foi possível remover o participante.";
          this.acaoEmAndamento = false;
        },
      });
  }
  sessaoEditavel(sessao: SessaoGrupoPayload): boolean {
    return sessao.status === "AGENDADA" && sessao.dataSessao >= this.hoje();
  }
  private atualizarSessao(sessao: SessaoGrupoPayload): void {
    this.sessoes = this.sessoes.map((s) => (s.id === sessao.id ? sessao : s));
    this.sessaoAberta = sessao;
  }
  private hoje(): string {
    const d = new Date();
    return this.dataISO(d);
  }
  private horaAtual(): string {
    return new Intl.DateTimeFormat("pt-BR", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date());
  }
  private somarDias(data: string, dias: number): string {
    const d = this.dataLocal(data);
    d.setDate(d.getDate() + dias);
    return this.dataISO(d);
  }
  private dataLocal(data: string): Date {
    const [a, m, d] = data.split("-").map(Number);
    return new Date(a, m - 1, d);
  }
  private dataISO(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(
      2,
      "0"
    )}-${String(d.getDate()).padStart(2, "0")}`;
  }
}

import {
    CommonModule
} from '@angular/common';
import {
    HttpErrorResponse
} from '@angular/common/http';
import {
    Component,
    OnDestroy,
    OnInit
} from '@angular/core';
import {
    FormsModule
} from '@angular/forms';
import {
    ActivatedRoute,
    Router
} from '@angular/router';

import {
    Subject,
    catchError,
    debounceTime,
    distinctUntilChanged,
    finalize,
    map,
    of,
    switchMap,
    takeUntil
} from 'rxjs';

import {
    ProfissionalPayload,
    ProfissionalService
} from '../../services/profissional/profissional-service';
import {
    AgendamentoService,
    HorarioDisponivel,
    HorariosDisponiveisPayload,
    NovoAgendamentoPayload,
    StandardError,
    ValidationError,
    VagasPorTurno
} from '../../services/agendamento-service';

@Component({
    selector: 'app-novo-agendamento',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './novo-agendamento.html',
    styleUrl: './novo-agendamento.css'
})
export class NovoAgendamento implements OnInit, OnDestroy {
    termoPesquisaPaciente = '';
    resultadosPacientes: PacientePayload[] = [];
    pacienteSelecionado: PacientePayload | null = null;
    buscandoPaciente = false;
    pesquisaRealizada = false;
    profissionais: ProfissionalPayload[] = [];
    profissionalSelecionadoId: string | null = null;
    dataSelecionada = '';
    vagas: VagasPorTurno | null = null;
    consultandoVagas = false;
    turnoSelecionado: string | null = null;
    horarios: HorarioDisponivel[] = [];
    horariosPayload: HorariosDisponiveisPayload | null = null;
    consultandoHorarios = false;
    erroHorarios: string | null = null;
    horaAtendimento = '';
    agendamentoOriginalId: number | null = null;
    carregandoRemarcacao = false;
    sugestoesRemarcacao: string[] = [];
    salvando = false;
    erroGeral: string | null = null;
    errosPorCampo: Record < string, string > = {};
    readonly dataMinima = this.dataLocalHoje();
    private pesquisa$ = new Subject < string > ();
    private horarioConsulta$ = new Subject < {
        usuarioId: string;data: string;turno: string
    } > ();
    private destroy$ = new Subject < void > ();

    constructor(private router: Router, private route: ActivatedRoute, private pacienteService: PacienteService, private profissionalService: ProfissionalService, private agendamentoService: AgendamentoService) {}

    ngOnInit(): void {
        this.configurarPesquisa();
        this.configurarConsultaHorarios();
        this.agendamentoOriginalId = Number(this.route.snapshot.queryParamMap.get('agendamentoOriginalId')) || null;
        this.carregarProfissionais();
        if (this.agendamentoOriginalId) this.carregarAgendamentoOriginal(this.agendamentoOriginalId);
    }
    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
    private configurarPesquisa(): void {
        this.pesquisa$.pipe(map(t => t.trim()), debounceTime(350), distinctUntilChanged(), switchMap(termo => {
            const digitos = termo.replace(/\D/g, '');
            const temLetras = /[A-Za-zÀ-ÿ]/.test(termo);
            this.pesquisaRealizada = false;
            if ((temLetras && termo.length < 3) || (!temLetras && ![11, 15].includes(digitos.length))) {
                this.buscandoPaciente = false;
                return of([]);
            }
            this.buscandoPaciente = true;
            const consulta = temLetras ? this.pacienteService.buscarPorNome(termo) : digitos.length === 11 ?
                this.pacienteService.buscarPorCpf(digitos).pipe(map(p => [p])) : this.pacienteService.buscarPorCns(digitos).pipe(map(p => [p]));
            return consulta.pipe(catchError(() => of([])), finalize(() => {
                this.buscandoPaciente = false;
                this.pesquisaRealizada = true;
            }));
        }), takeUntil(this.destroy$)).subscribe(p => this.resultadosPacientes = p);
    }

    import {
        CommonModule
    } from '@angular/common';
    import {
        HttpErrorResponse
    } from '@angular/common/http';
    import {
        Component,
        OnInit
    } from '@angular/core';
    import {
        Component,
        OnDestroy,
        OnInit
    } from '@angular/core';
    import {
        FormsModule
    } from '@angular/forms';
    import {
        ActivatedRoute,
        Router
    } from '@angular/router';

    import {
        Subject,
        catchError,
        debounceTime,
        distinctUntilChanged,
        finalize,
        map,
        of,
        switchMap,
        takeUntil
    } from 'rxjs';
    import {
        PacientePayload,
        PacienteService
    } from '../../services/paciente/paciente-service';
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

        agendamentoOriginalId: number | null = null;
        carregandoRemarcacao = false;
        sugestoesRemarcacao: string[] = [];

        salvando = false;
        erroGeral: string | null = null;
        errosPorCampo: Record < string, string > = {};

        constructor(
            private router: Router,
            private route: ActivatedRoute,
            private pacienteService: PacienteService,
            private profissionalService: ProfissionalService,
            private agendamentoService: AgendamentoService,
        ) {}

        import {
            ProfissionalPayload,
            ProfissionalService
        } from '../../services/profissional/profissional-service';
        import {
            AgendamentoService,
            HorarioDisponivel,
            HorariosDisponiveisPayload,
            NovoAgendamentoPayload,
            StandardError,
            ValidationError,
            VagasPorTurno
        } from '../../services/agendamento-service';

        @Component({
            selector: 'app-novo-agendamento',
            standalone: true,
            imports: [CommonModule, FormsModule],
            templateUrl: './novo-agendamento.html',
            styleUrl: './novo-agendamento.css'
        })
        export class NovoAgendamento implements OnInit, OnDestroy {
            termoPesquisaPaciente = '';
            resultadosPacientes: PacientePayload[] = [];
            pacienteSelecionado: PacientePayload | null = null;
            buscandoPaciente = false;
            pesquisaRealizada = false;
            profissionais: ProfissionalPayload[] = [];
            profissionalSelecionadoId: string | null = null;
            dataSelecionada = '';
            vagas: VagasPorTurno | null = null;
            consultandoVagas = false;
            turnoSelecionado: string | null = null;
            horarios: HorarioDisponivel[] = [];
            horariosPayload: HorariosDisponiveisPayload | null = null;
            consultandoHorarios = false;
            erroHorarios: string | null = null;
            horaAtendimento = '';
            agendamentoOriginalId: number | null = null;
            carregandoRemarcacao = false;
            sugestoesRemarcacao: string[] = [];
            salvando = false;
            erroGeral: string | null = null;
            errosPorCampo: Record < string, string > = {};
            readonly dataMinima = this.dataLocalHoje();
            private pesquisa$ = new Subject < string > ();
            private horarioConsulta$ = new Subject < {
                usuarioId: string;data: string;turno: string
            } > ();
            private destroy$ = new Subject < void > ();

            constructor(private router: Router, private route: ActivatedRoute, private pacienteService: PacienteService, private profissionalService: ProfissionalService, private agendamentoService: AgendamentoService) {}
            ngOnInit(): void {
                this.configurarPesquisa();
                this.configurarConsultaHorarios();
                this.agendamentoOriginalId = Number(this.route.snapshot.queryParamMap.get('agendamentoOriginalId')) || null;
                this.carregarProfissionais();
                if (this.agendamentoOriginalId) {
                    this.carregarAgendamentoOriginal(this.agendamentoOriginalId);
                }
                this.carregarProfissionais();
                if (this.agendamentoOriginalId) this.carregarAgendamentoOriginal(this.agendamentoOriginalId);
            }
            carregarAgendamentoOriginal(id: number): void {
                    this.carregandoRemarcacao = true;
                    this.agendamentoService.buscarPorId(id).subscribe({
                        next: (agendamento) => {
                            this.pacienteSelecionado = {
                                idPublico: agendamento.pacienteId,
                                nome: agendamento.nomePaciente,
                                tipoAcompanhamento: agendamento.tipoAcompanhamento
                            }
                            as PacientePayload;
                            this.profissionalSelecionadoId = agendamento.usuarioId;
                            this.turnoSelecionado = agendamento.turnoAgendamento;
                            this.carregandoRemarcacao = false;
                            this.agendamentoService.sugerirDatasRemarcacao(id).subscribe({
                                next: (datas) => this.sugestoesRemarcacao = datas,
                                error: () => this.sugestoesRemarcacao = []
                            });
                        },
                        error: (erro) => {
                            console.error('Erro ao carregar agendamento original', erro);
                            this.erroGeral = 'Não foi possível carregar o agendamento original para remarcação.';
                            this.carregandoRemarcacao = false;
                        },
                    });
                    ngOnDestroy(): void {
                        this.destroy$.next();
                        this.destroy$.complete();
                    }
                    private configurarPesquisa(): void {
                        this.pesquisa$.pipe(map(t => t.trim()), debounceTime(350), distinctUntilChanged(), switchMap(termo => {
                            const digitos = termo.replace(/\D/g, '');
                            const temLetras = /[A-Za-zÀ-ÿ]/.test(termo);
                            this.pesquisaRealizada = false;
                            if ((temLetras && termo.length < 3) || (!temLetras && ![11, 15].includes(digitos.length))) {
                                this.buscandoPaciente = false;
                                return of([]);
                            }
                            this.buscandoPaciente = true;
                            const consulta = temLetras ? this.pacienteService.buscarPorNome(termo) : digitos.length === 11 ?
                                this.pacienteService.buscarPorCpf(digitos).pipe(map(p => [p])) : this.pacienteService.buscarPorCns(digitos).pipe(map(p => [p]));
                            return consulta.pipe(catchError(() => of([])), finalize(() => {
                                this.buscandoPaciente = false;
                                this.pesquisaRealizada = true;
                            }));
                        }), takeUntil(this.destroy$)).subscribe(p => this.resultadosPacientes = p);
                    }

                    get modoRemarcacao(): boolean {
                        return this.agendamentoOriginalId !== null;
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
                            agendamentoOriginalId: this.agendamentoOriginalId ?? undefined,
                        };

                        this.agendamentoService.criarAgendamento(payload).subscribe({
                            next: () => {
                                this.salvando = false;
                                this.router.navigate(['/agenda'], {
                                    queryParams: {
                                        data: this.dataSelecionada
                                    },
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
                        private configurarConsultaHorarios(): void {
                            this.horarioConsulta$.pipe(switchMap(c => {
                                this.consultandoHorarios = true;
                                this.erroHorarios = null;
                                return this.agendamentoService.buscarHorariosDisponiveis(c.usuarioId, c.data, c.turno).pipe(catchError((e: HttpErrorResponse) => {
                                    this.erroHorarios = e.error?.message || 'Não foi possível consultar os horários.';
                                    return of(null);
                                }), finalize(() => this.consultandoHorarios = false));
                            }), takeUntil(this.destroy$)).subscribe(payload => {
                                this.horariosPayload = payload;
                                this.horarios = payload?.horarios ?? [];
                            });
                        }
                        pesquisarPaciente(): void {
                            this.pesquisa$.next(this.termoPesquisaPaciente);
                        }
                        selecionarPaciente(p: PacientePayload): void {
                            this.pacienteSelecionado = p;
                            this.resultadosPacientes = [];
                            this.termoPesquisaPaciente = '';
                        }
                        trocarPaciente(): void {
                            if (!this.modoRemarcacao) this.pacienteSelecionado = null;
                        }
                        carregarProfissionais(): void {
                            this.profissionalService.listar().subscribe({
                                next: p => this.profissionais = p.filter(x => x.tipoUsuario !== 'RECEPCAO'),
                                error: () => this.erroGeral = 'Não foi possível carregar a lista de profissionais.'
                            });
                        }
                        carregarAgendamentoOriginal(id: number): void {
                            this.carregandoRemarcacao = true;
                            this.agendamentoService.buscarPorId(id).subscribe({
                                next: a => {
                                    this.profissionalSelecionadoId = a.usuarioId;
                                    this.pacienteService.buscarPorId(a.pacienteId).subscribe({
                                        next: p => this.pacienteSelecionado = p,
                                        error: () => this.pacienteSelecionado = {
                                            idPublico: a.pacienteId,
                                            nome: a.nomePaciente,
                                            tipoAcompanhamento: a.tipoAcompanhamento
                                        }
                                        as PacientePayload
                                    });
                                    this.carregandoRemarcacao = false;
                                    this.agendamentoService.sugerirDatasRemarcacao(id).subscribe({
                                        next: d => this.sugestoesRemarcacao = d
                                    });
                                },
                                error: () => {
                                    this.erroGeral = 'Não foi possível carregar o agendamento original para remarcação.';
                                    this.carregandoRemarcacao = false;
                                }
                            });
                        }
                        get modoRemarcacao(): boolean {
                            return this.agendamentoOriginalId !== null;
                        }
                        onProfissionalOuDataAlterado(): void {
                            this.vagas = null;
                            this.turnoSelecionado = null;
                            this.limparHorario();
                            if (!this.profissionalSelecionadoId || !this.dataSelecionada) return;
                            this.consultandoVagas = true;
                            this.agendamentoService.consultarVagas(this.profissionalSelecionadoId, this.dataSelecionada).pipe(finalize(() => this.consultandoVagas = false)).subscribe({
                                next: v => this.vagas = v,
                                error: () => this.erroGeral = 'Não foi possível consultar as vagas disponíveis.'
                            });
                        }
                        selecionarDataSugerida(data: string): void {
                            this.dataSelecionada = data;
                            this.onProfissionalOuDataAlterado();
                        }
                        selecionarTurno(turno: string): void {
                            if (this.vagasDoTurno(turno) <= 0 || !this.profissionalSelecionadoId || !this.dataSelecionada) return;
                            this.turnoSelecionado = turno;
                            this.limparHorario();
                            this.horarioConsulta$.next({
                                usuarioId: this.profissionalSelecionadoId,
                                data: this.dataSelecionada,
                                turno
                            });
                        }
                        selecionarHorario(h: HorarioDisponivel): void {
                            if (h.disponivel) this.horaAtendimento = h.hora.slice(0, 5);
                        }
                        private limparHorario(): void {
                            this.horaAtendimento = '';
                            this.horarios = [];
                            this.horariosPayload = null;
                            this.erroHorarios = null;
                        }
                        vagasDoTurno(t: string): number {
                            return !this.vagas ? 0 : t === 'MANHA' ? this.vagas.MANHA : this.vagas.TARDE;
                        }
                        get podeConfirmar(): boolean {
                            return !!(this.pacienteSelecionado && this.profissionalSelecionadoId && this.dataSelecionada && this.turnoSelecionado && this.horaAtendimento && !this.salvando);
                        }
                        confirmarAgendamento(): void {
                            if (!this.podeConfirmar) return;
                            this.salvando = true;
                            this.erroGeral = null;
                            const payload: NovoAgendamentoPayload = {
                                usuarioId: this.profissionalSelecionadoId!,
                                pacienteId: this.pacienteSelecionado!.idPublico!,
                                dataAgendamento: this.dataSelecionada,
                                turnoAgendamento: this.turnoSelecionado!,
                                horaAtendimento: this.horaAtendimento,
                                agendamentoOriginalId: this.agendamentoOriginalId ?? undefined
                            };
                            this.agendamentoService.criarAgendamento(payload).subscribe({
                                next: () => this.router.navigate(['/agenda'], {
                                    queryParams: {
                                        data: this.dataSelecionada
                                    }
                                }),
                                error: (e: HttpErrorResponse) => {
                                    this.salvando = false;
                                    this.tratarErro(e);
                                }
                            });
                        }
                        private tratarErro(e: HttpErrorResponse): void {
                            if (e.status === 422 && Array.isArray(e.error?.errors)) {
                                const v = e.error as ValidationError;
                                v.errors.forEach(x => this.errosPorCampo[x.fieldName] = x.message);
                            }
                            this.erroGeral = (e.error as StandardError)?.message || 'Não foi possível criar o agendamento.';
                        }
                        cancelar(): void {
                            this.router.navigate(['/agenda']);
                        }
                        profissionalSelecionado(): ProfissionalPayload | undefined {
                            return this.profissionais.find(p => p.idPublico === this.profissionalSelecionadoId);
                        }
                        labelEnum(v ? : string): string {
                            return v ? v.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) : 'Não selecionado';
                        }
                        formatarCpf(v ? : string): string {
                            const n = v?.replace(/\D/g, '') || '';
                            return n.length === 11 ? n.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4') : 'Não informado';
                        }
                        idade(data ? : string): number | null {
                            if (!data) return null;
                            const n = new Date();
                            const d = new Date(`${data}T00:00:00`);
                            let i = n.getFullYear() - d.getFullYear();
                            if (n < new Date(n.getFullYear(), d.getMonth(), d.getDate())) i--;
                            return i;
                        }
                        formatarData(data ? : string): string {
                            if (!data) return 'Não selecionado';
                            return new Intl.DateTimeFormat('pt-BR', {
                                dateStyle: 'long'
                            }).format(new Date(`${data}T00:00:00`));
                        }
                        mensagemHorarios(): string {
                            const m = this.horariosPayload?.motivoIndisponibilidade;
                            return m === 'AGENDA_BLOQUEADA' ? 'A agenda do profissional está bloqueada nesta data.' : m === 'TURNO_NAO_CONFIGURADO' ? 'Este profissional não possui disponibilidade neste turno.' : m === 'CAPACIDADE_ESGOTADA' ? 'Não há mais vagas disponíveis neste turno.' : 'Nenhum horário disponível para este turno.';
                        }
                        private dataLocalHoje(): string {
                            const d = new Date();
                            const z = (n: number) => String(n).padStart(2, '0');
                            return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
                        }
                    }
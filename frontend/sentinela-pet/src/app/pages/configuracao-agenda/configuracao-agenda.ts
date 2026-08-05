import {
    CommonModule
} from '@angular/common';
import {
    HttpErrorResponse
} from '@angular/common/http';

import {
    Component,
    HostListener,
    OnInit
} from '@angular/core';
import {
    FormsModule
} from '@angular/forms';
import {
    forkJoin
} from 'rxjs';

import {
    BloqueioAgendaDTO,
    BloqueioAgendaService
} from '../../services/bloqueio-agenda-service';
import {
    DisponibilidadeExcecaoDTO,
    DisponibilidadeExcecaoService
} from '../../services/disponibilidade-excecao-service';
import {
    DisponibilidadeDTO,
    DisponibilidadeService,
    StandardError
} from '../../services/disponibilidade-service';
import {
    ProfissionalPayload,
    ProfissionalService
} from '../../services/profissional/profissional-service';
import {
    UsuarioLogadoService
} from '../../services/usuario-logado-service';

type Aba = 'horarios' | 'bloqueios' | 'excecoes';
type Modal = 'horario' | 'bloqueio' | 'excecao' | 'confirmacao' | null;
type AcaoExclusao = {
    tipo: Aba;id: number
};

@Component({
    selector: 'app-configuracao-agenda',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './configuracao-agenda.html',
    styleUrl: './configuracao-agenda.css',
})
export class ConfiguracaoAgenda implements OnInit {
    readonly diasSemana = [{
            valor: 'MONDAY',
            label: 'Segunda-feira',
            curto: 'SEG'
        }, {
            valor: 'TUESDAY',
            label: 'Terça-feira',
            curto: 'TER'
        },
        {
            valor: 'WEDNESDAY',
            label: 'Quarta-feira',
            curto: 'QUA'
        }, {
            valor: 'THURSDAY',
            label: 'Quinta-feira',
            curto: 'QUI'
        },
        {
            valor: 'FRIDAY',
            label: 'Sexta-feira',
            curto: 'SEX'
        }, {
            valor: 'SATURDAY',
            label: 'Sábado',
            curto: 'SÁB'
        },
        {
            valor: 'SUNDAY',
            label: 'Domingo',
            curto: 'DOM'
        },
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
        capacidade: 1
    };
    bloqueioForm: BloqueioAgendaDTO = {
        dataInicio: '',
        dataFim: '',
        motivoBloqueio: ''
    };
    excecaoForm: DisponibilidadeExcecaoDTO = {
        data: '',
        turno: '',
        capacidade: 1
    };
    acaoExclusao: AcaoExclusao | null = null;
    elementoOrigem: HTMLElement | null = null;

    constructor(private usuarioLogadoService: UsuarioLogadoService, private profissionalService: ProfissionalService,
        private disponibilidadeService: DisponibilidadeService, private bloqueioAgendaService: BloqueioAgendaService,
        private disponibilidadeExcecaoService: DisponibilidadeExcecaoService) {}

    ngOnInit(): void {
        this.usuarioLogadoService.obterUsuarioLogado().subscribe({
            next: usuario => {
                this.isAdmin = usuario.tipoUsuario === 'ADMINISTRADOR';
                this.isProfissional = usuario.tipoUsuario === 'PROFISSIONAL';
                this.nomeUsuario = usuario.nome;
                this.carregandoUsuarioLogado = false;
                if (this.isAdmin) this.carregarProfissionais();
                else this.carregarDados();
            },
            error: () => {
                this.carregandoUsuarioLogado = false;
                this.erroGeral = 'Não foi possível identificar o usuário logado.';
            }
        });
    }

    carregarProfissionais(): void {
        this.profissionalService.listar().subscribe({
            next: ps => this.profissionais = ps.filter(p => p.tipoUsuario === 'PROFISSIONAL'),
            error: () => this.erroGeral = 'Não foi possível carregar a lista de profissionais.'
        });
    }
    onProfissionalSelecionado(): void {
        this.limparDados();
        if (this.profissionalSelecionadoId) this.carregarDados(this.profissionalSelecionadoId);
    }
    limparDados(): void {
        this.disponibilidades = [];
        this.bloqueios = [];
        this.excecoes = [];
        this.erroGeral = null;
        this.sucesso = null;
    }
}

carregarDados(usuarioId ? : string): void {
    this.carregandoDados = true;
    this.erroGeral = null;
    forkJoin({
        disponibilidades: this.disponibilidadeService.listar(usuarioId),
        bloqueios: this.bloqueioAgendaService.listar(usuarioId),
        excecoes: this.disponibilidadeExcecaoService.listar(usuarioId)
    }).subscribe({
        next: dados => {
            this.disponibilidades = this.ordenarHorarios(dados.disponibilidades);
            this.bloqueios = dados.bloqueios;
            this.excecoes = dados.excecoes.sort((a, b) => a.data.localeCompare(b.data));
            this.carregandoDados = false;
        },
        error: (e: HttpErrorResponse) => {
            this.carregandoDados = false;
            this.erroGeral = this.extrairMensagemErro(e, 'Não foi possível carregar a configuração da agenda.');
        }
    });
}
get usuarioIdAtivo(): string | undefined {
    return this.isAdmin ? this.profissionalSelecionadoId ?? undefined : undefined;
}
get podeGerenciar(): boolean {
    return (this.isAdmin ? !!this.profissionalSelecionadoId : this.isProfissional) && !this.carregandoDados;
}
selecionarAba(aba: Aba): void {
    this.abaAtiva = aba;
}
navegarAbas(event: KeyboardEvent): void {
    const abas: Aba[] = ['horarios', 'bloqueios', 'excecoes'];
    const i = abas.indexOf(this.abaAtiva);
    if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
        event.preventDefault();
        this.abaAtiva = abas[(i + (event.key === 'ArrowRight' ? 1 : 2)) % 3];
        setTimeout(() => document.getElementById(`tab-${this.abaAtiva}`)?.focus());
    }
}

abrirHorario(d ? : DisponibilidadeDTO, origem ? : Event): void {
    this.prepararModal(origem);
    this.disponibilidadeForm = d ? {
        ...d
    } : {
        diaSemana: '',
        turno: '',
        capacidade: 1
    };
    this.modal = 'horario';
}
abrirBloqueio(b ? : BloqueioAgendaDTO, origem ? : Event): void {
    this.prepararModal(origem);
    this.bloqueioForm = b ? {
        ...b
    } : {
        dataInicio: '',
        dataFim: '',
        motivoBloqueio: ''
    };
    this.modal = 'bloqueio';
}
abrirExcecao(e ? : DisponibilidadeExcecaoDTO, origem ? : Event): void {
    this.prepararModal(origem);
    this.excecaoForm = e ? {
        ...e
    } : {
        data: '',
        turno: '',
        capacidade: 1
    };
    this.modal = 'excecao';
}
confirmarExclusao(tipo: Aba, id: number | undefined, origem ? : Event): void {
    if (!id) return;
    this.prepararModal(origem);
    this.acaoExclusao = {
        tipo,
        id
    };
    this.modal = 'confirmacao';
}
prepararModal(origem ? : Event): void {
    this.elementoOrigem = origem?.currentTarget as HTMLElement ?? null;
    this.erroModal = null;
    this.sucesso = null;
    setTimeout(() => document.querySelector < HTMLElement > ('.modal-card input:not([readonly]), .modal-card select:not([disabled]), .modal-card button')?.focus());
}
fecharModal(): void {
    if (this.salvando) return;
    this.modal = null;
    this.erroModal = null;
    setTimeout(() => this.elementoOrigem?.focus());
}
@HostListener('document:keydown.escape') aoEscape(): void {
    if (this.modal) this.fecharModal();
}
prenderFoco(event: KeyboardEvent): void {
    if (event.key !== 'Tab') return;
    const itens = Array.from(document.querySelectorAll < HTMLElement > ('.modal-card button:not([disabled]), .modal-card input:not([disabled]), .modal-card select:not([disabled])'));
    if (!itens.length) return;
    const primeiro = itens[0],
        ultimo = itens[itens.length - 1];
    if (event.shiftKey && document.activeElement === primeiro) {
        event.preventDefault();
        ultimo.focus();
    } else if (!event.shiftKey && document.activeElement === ultimo) {
        event.preventDefault();
        primeiro.focus();
    }
}

salvarHorario(): void {
    const f = this.disponibilidadeForm;
    if (!f.diaSemana || !f.turno || !Number.isInteger(f.capacidade) || f.capacidade < 1) {
        this.erroModal = 'Informe dia, turno e uma quantidade inteira de vagas maior que zero.';
        return;
    }
    this.executar(this.disponibilidadeService.salvar({
        ...f,
        usuarioId: this.usuarioIdAtivo
    }), f.id ? 'Horário atualizado com sucesso.' : 'Horário adicionado com sucesso.');
}
salvarBloqueio(): void {
    const f = this.bloqueioForm;
    if (!f.dataInicio || !f.dataFim || !f.motivoBloqueio?.trim()) {
        this.erroModal = 'Preencha as datas e o motivo do bloqueio.';
        return;
    }
    if (f.dataFim < f.dataInicio) {
        this.erroModal = 'A data final não pode ser anterior à data inicial.';
        return;
    }
    this.executar(this.bloqueioAgendaService.salvar({
        ...f,
        motivoBloqueio: f.motivoBloqueio.trim(),
        usuarioId: this.usuarioIdAtivo
    }), f.id ? 'Bloqueio atualizado com sucesso.' : 'Bloqueio criado com sucesso.');
}
salvarExcecao(): void {
    const f = this.excecaoForm;
    if (!f.data || !f.turno || !Number.isInteger(f.capacidade) || f.capacidade < 0) {
        this.erroModal = 'Informe data, turno e uma capacidade inteira igual ou maior que zero.';
        return;
    }
    this.executar(this.disponibilidadeExcecaoService.salvar({
        ...f,
        usuarioId: this.usuarioIdAtivo
    }), f.id ? 'Exceção atualizada com sucesso.' : 'Exceção criada com sucesso.');
}
executar(obs: any, mensagem: string): void {
    if (this.salvando) return;
    this.salvando = true;
    this.erroModal = null;
    obs.subscribe({
        next: () => {
            this.salvando = false;
            this.modal = null;
            this.sucesso = mensagem;
            this.carregarDados(this.usuarioIdAtivo);
            setTimeout(() => this.elementoOrigem?.focus());
        },
        error: (e: HttpErrorResponse) => {
            this.salvando = false;
            this.erroModal = this.extrairMensagemErro(e, 'Não foi possível concluir a operação.');
        }
    });
}
excluir(): void {
    if (!this.acaoExclusao || this.salvando) return;
    const {
        tipo,
        id
    } = this.acaoExclusao;
    const obs = tipo === 'horarios' ? this.disponibilidadeService.remover(id) : tipo === 'bloqueios' ? this.bloqueioAgendaService.remover(id) : this.disponibilidadeExcecaoService.remover(id);
    const msg = tipo === 'horarios' ? 'Horário excluído com sucesso.' : tipo === 'bloqueios' ? 'Bloqueio excluído com sucesso.' : 'Exceção excluída com sucesso.';
    this.executar(obs, msg);
}

capacidade(dia: string, turno: string): number | null {
    return this.disponibilidades.find(d => d.diaSemana === dia && d.turno === turno)?.capacidade ?? null;
}
labelDiaSemana(v: string): string {
    return this.diasSemana.find(d => d.valor === v)?.label ?? v;
}
labelTurno(v: string): string {
    return v === 'MANHA' ? 'Manhã' : 'Tarde';
}
formatarData(data: string): string {
    if (!data) return '-';
    const [a, m, d] = data.split('-');
    return `${d}/${m}/${a}`;
}
periodo(b: BloqueioAgendaDTO): string {
    return b.dataInicio === b.dataFim ? this.formatarData(b.dataInicio) : `${this.formatarData(b.dataInicio)} até ${this.formatarData(b.dataFim)}`;
}
private ordenarHorarios(ds: DisponibilidadeDTO[]): DisponibilidadeDTO[] {
    return [...ds].sort((a, b) => {
        const da = this.diasSemana.findIndex(d => d.valor === a.diaSemana),
            db = this.diasSemana.findIndex(d => d.valor === b.diaSemana);
        return da - db || (a.turno === 'MANHA' ? -1 : 1);
    });
}
private extrairMensagemErro(e: HttpErrorResponse, p: string): string {
    return (e.error as StandardError)?.message || p;
}

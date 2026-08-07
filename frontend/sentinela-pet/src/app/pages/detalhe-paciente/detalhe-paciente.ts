import {
    CommonModule
} from '@angular/common';
import {
    Component,
    HostListener,
    OnInit
} from '@angular/core';
import {
    FormsModule
} from '@angular/forms';
import {
    ActivatedRoute,
    Router,
    RouterLink
} from '@angular/router';
import {
    PacienteDetalhe,
    PacienteService
} from '../../services/paciente/paciente-service';
import {
    UsuarioLogadoService
} from '../../services/usuario-logado-service';
@Component({
    selector: 'app-detalhe-paciente',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './detalhe-paciente.html',
    styleUrl: './detalhe-paciente.css'
})
export class DetalhePaciente implements OnInit {
    detalhe ? : PacienteDetalhe;
    id = '';
    carregando = true;
    erro = '';
    modal: '' | 'encerrar' | 'busca' = '';
    motivo = '';
    descricao = '';
    salvando = false;
    podeGerenciar = false;
    constructor(private route: ActivatedRoute, private router: Router, private service: PacienteService, usuario: UsuarioLogadoService) {
        usuario.obterUsuarioLogado().subscribe(u => this.podeGerenciar = ['ADMINISTRADOR', 'PROFISSIONAL'].includes(u.tipoUsuario))
    }
    ngOnInit() {
        this.id = this.route.snapshot.paramMap.get('id') || '';
        if (!this.id) {
            this.erro = 'Paciente não informado';
            return
        }
        this.carregar()
    }
    carregar() {
        this.service.buscarDetalhe(this.id).subscribe({
            next: d => {
                this.detalhe = d;
                this.carregando = false
            },
            error: () => {
                this.erro = 'Não foi possível carregar os dados deste paciente.';
                this.carregando = false
            }
        })
    }
    get p() {
        return this.detalhe?.paciente
    }
    get ativo() {
        return this.p?.statusPaciente === 'ATIVO'
    }
    iniciais() {
        return (this.p?.nome || '').split(/\s+/).slice(0, 2).map(x => x[0]).join('').toUpperCase()
    }
    idade() {
        if (!this.p) return 0;
        const n = new Date(this.p.dataNascimento + 'T00:00:00'),
            h = new Date();
        let i = h.getFullYear() - n.getFullYear();
        if (h < new Date(h.getFullYear(), n.getMonth(), n.getDate())) i--;
        return i
    }
    label(v ? : string) {
        return (v || 'Não informado').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, x => x.toUpperCase())
    }
    data(v ? : string) {
        return v ? new Intl.DateTimeFormat('pt-BR').format(new Date(v + 'T00:00:00')) : 'Sem presença registrada'
    }
    mask(v: string | undefined, t: 'cpf' | 'cns') {
        const d = (v || '').replace(/\D/g, '');
        return t === 'cpf' ? `•••.${d.slice(3,6)}.${d.slice(6,9)}-••` : `•••••••••${d.slice(-6)}`
    }
    encerrar() {
        if (!this.motivo || this.motivo === 'OUTRO' && !this.descricao.trim()) return;
        this.salvando = true;
        this.service.encerrar(this.id, this.motivo, this.descricao).subscribe({
            next: () => {
                this.modal = '';
                this.salvando = false;
                this.carregar()
            },
            error: e => {
                this.erro = e.error?.message || 'Não foi possível encerrar.';
                this.salvando = false
            }
        })
    }
    registrar() {
        if (!this.descricao.trim()) return;
        this.salvando = true;
        this.service.registrarBuscaAtiva(this.id, this.descricao).subscribe({
            next: () => {
                this.modal = '';
                this.descricao = '';
                this.salvando = false
            },
            error: () => {
                this.erro = 'Não foi possível registrar a busca ativa.';
                this.salvando = false
            }
        })
    }
    abrirHistoricoPaciente() {
        this.router.navigate(['/pacientes/detalhes', this.id, 'historico'])
    }
    @HostListener('document:keydown.escape') escape() {
        this.modal = ''
    }
}

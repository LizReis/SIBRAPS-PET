import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { EvolucaoPainel, FiltrosPainel, PainelPayload, PainelService } from '../../services/painel-service';

@Component({
  selector: 'app-painel',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './painel.html',
  styleUrl: './painel.css',
})
export class Painel implements OnInit {
  private readonly painelService = inject(PainelService);
  dados?: PainelPayload;
  carregando = true;
  erro = false;
  maisFiltrosAberto = false;
  filtros: FiltrosPainel = { periodoMeses: 6 };
  rascunhoTipo = '';
  rascunhoRua = '';

  readonly riscos = [
    { chave: 'vermelhos', percentual: 'percentualVermelhos', titulo: 'Busca ativa', classe: 'red', icone: 'warning' },
    { chave: 'amarelos', percentual: 'percentualAmarelos', titulo: 'Atenção', classe: 'yellow', icone: 'warning' },
    { chave: 'verdes', percentual: 'percentualVerdes', titulo: 'Regular', classe: 'green', icone: 'check_circle' },
  ] as const;

  ngOnInit(): void { this.carregar(); }
  carregar(): void {
    this.carregando = true; this.erro = false;
    this.painelService.buscarResumo(this.filtros).pipe(finalize(() => this.carregando = false)).subscribe({
      next: dados => this.dados = dados,
      error: () => this.erro = true,
    });
  }
  alterarFiltroPrincipal(): void { this.carregar(); }
  aplicarMaisFiltros(): void {
    this.filtros = { ...this.filtros,
      tipoAcompanhamento: this.rascunhoTipo || undefined,
      situacaoRua: this.rascunhoRua === '' ? undefined : this.rascunhoRua === 'true' };
    this.maisFiltrosAberto = false; this.carregar();
  }
  limparMaisFiltros(): void { this.rascunhoTipo = ''; this.rascunhoRua = ''; }
  valor(chave: 'verdes'|'amarelos'|'vermelhos'): number { return this.dados?.distribuicaoClassificacao[chave] ?? 0; }
  percentual(chave: 'percentualVerdes'|'percentualAmarelos'|'percentualVermelhos'): number { return this.dados?.distribuicaoClassificacao[chave] ?? 0; }
  formatarPercentual(valor: number): string { return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valor); }
  larguraBarra(valor: number): number { return this.dados?.totalPacientesAtivos ? valor / this.dados.totalPacientesAtivos * 100 : 0; }
  iniciais(nome: string): string { return nome.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase(); }
  rotuloRisco(risco: string): string { return risco === 'VERMELHO' ? 'Busca ativa' : risco === 'AMARELO' ? 'Atenção' : 'Regular'; }

  get maxEvolucao(): number {
    const valores = (this.dados?.evolucao ?? []).flatMap(p => [p.verdes, p.amarelos, p.vermelhos]);
    return Math.max(1, ...valores);
  }
  pontos(campo: keyof Pick<EvolucaoPainel, 'verdes'|'amarelos'|'vermelhos'>): string {
    const serie = this.dados?.evolucao ?? [];
    if (!serie.length) return '';
    return serie.map((p, i) => `${this.xPonto(i, serie.length)},${this.yPonto(p[campo])}`).join(' ');
  }
  xPonto(indice:number, total:number): number { return total <= 1 ? 50 : 8 + indice * 84 / (total - 1); }
  yPonto(valor:number): number { return 84 - valor * 70 / this.maxEvolucao; }
  tooltip(p:EvolucaoPainel): string { return `${p.rotulo}: Regular ${p.verdes}, Atenção ${p.amarelos}, Busca ativa ${p.vermelhos}`; }

}

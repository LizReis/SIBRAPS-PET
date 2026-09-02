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
  styleUrls: ['./painel.css', './painel-ajustes.css'],
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
  larguraBarra(valor: number): number { return this.maxSituacao ? valor / this.maxSituacao * 100 : 0; }
  iniciais(nome: string): string { return nome.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase(); }
  rotuloRisco(risco: string): string { return risco === 'VERMELHO' ? 'Busca ativa' : risco === 'AMARELO' ? 'Atenção' : 'Regular'; }

  get maxSituacao(): number {
    return this.tetoEscala(this.dados?.totalPacientesAtivos ?? 0);
  }

  get ticksSituacao(): number[] {
    return Array.from({ length: 6 }, (_, i) => Math.round((this.maxSituacao * i) / 5));
  }

  get maxEvolucao(): number {
    const valores = (this.dados?.evolucao ?? []).flatMap(p => [p.verdes, p.amarelos, p.vermelhos]);
    return Math.max(1, ...valores);
  }

  get maxEvolucaoEscala(): number {
    return this.tetoEscala(this.maxEvolucao);
  }

  get ticksEvolucao(): { valor: number; y: number }[] {
    const topo = this.maxEvolucaoEscala;
    return Array.from({ length: 5 }, (_, i) => ({
      valor: Math.round(topo - (topo * i) / 4),
      y: 14 + i * 17.5,
    }));
  }

  pontos(campo: keyof Pick<EvolucaoPainel, 'verdes'|'amarelos'|'vermelhos'>): string {
    const serie = this.dados?.evolucao ?? [];
    if (!serie.length) return '';
    return serie.map((p, i) => `${this.xPonto(i, serie.length)},${this.yPonto(p[campo])}`).join(' ');
  }

  xPonto(indice: number, total: number): number { return total <= 1 ? 50 : 10 + indice * 82 / (total - 1); }
  yPonto(valor: number): number { return 84 - valor * 70 / this.maxEvolucaoEscala; }

  trianguloPonto(x: number, y: number): string {
    return `${x},${y - 1.7} ${x - 1.6},${y + 1.3} ${x + 1.6},${y + 1.3}`;
  }

  tooltip(p: EvolucaoPainel): string { return `${p.rotulo}: Regular ${p.verdes}, Atenção ${p.amarelos}, Busca ativa ${p.vermelhos}`; }

  private tetoEscala(valor: number): number {
    if (valor <= 0) return 5;
    const magnitude = Math.pow(10, Math.floor(Math.log10(valor)));
    const normalizado = valor / magnitude;
    const fator = normalizado <= 1 ? 1 : normalizado <= 2 ? 2 : normalizado <= 2.5 ? 2.5 : normalizado <= 5 ? 5 : 10;
    return Math.max(5, fator * magnitude);
  }
}

import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

type TipoRelatorio = 'buscaAtiva' | 'frequenciaGrupo';
type FormatoExportacao = 'PDF' | 'Excel' | 'CSV';

interface PacienteBuscaAtivaMock {
  nome: string;
  documento: string;
  dataUltimoAtendimento: string;
  faltasConsecutivas: number;
  tipoAcompanhamento: 'Individual' | 'Grupo terapêutico';
  telefone: string;
  profissional: string;
}

interface SessaoFrequenciaMock {
  id: number;
  grupo: string;
  data: string;
  profissionalResponsavel: string;
  presentes: string[];
  ausentes: string[];
}

@Component({
  selector: 'app-relatorios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './relatorios.html',
  styleUrl: './relatorios.css',
})
export class Relatorios {
  abaAtiva: TipoRelatorio = 'buscaAtiva';
  formatoSelecionado: FormatoExportacao = 'CSV';
  periodoBusca = 'Últimos 6 meses';
  profissionalSelecionado = 'Todos os profissionais';
  tipoAcompanhamentoSelecionado = 'Individual e grupo';
  grupoSelecionado = 'Todos os grupos';
  periodoFrequencia = 'Últimos 6 meses';

  readonly periodos = [
    'Últimos 30 dias',
    'Últimos 3 meses',
    'Últimos 6 meses',
    'Últimos 12 meses',
    'Todo o período',
  ];
  readonly tiposAcompanhamento = ['Individual e grupo', 'Individual', 'Grupo terapêutico'];
  readonly profissionais = [
    'Todos os profissionais',
    'Dra. Marina Costa',
    'João Ferreira',
    'Ana Oliveira',
  ];
  readonly grupos = [
    'Todos os grupos',
    'Grupo de Convivência',
    'Grupo de Cuidado em Álcool e Drogas',
    'Grupo de Familiares',
  ];

  // TODO RF21: substituir dados mockados por dados reais da API de relatórios.
  readonly pacientesBuscaAtiva: PacienteBuscaAtivaMock[] = [
    {
      nome: 'Luciana Almeida', documento: '708 4012 3456 7890', dataUltimoAtendimento: '12/06/2026',
      faltasConsecutivas: 4, tipoAcompanhamento: 'Individual', telefone: '(81) 99812-4471',
      profissional: 'Dra. Marina Costa',
    },
    {
      nome: 'Marcos Vinícius Lima', documento: '122.556.890-04', dataUltimoAtendimento: '19/06/2026',
      faltasConsecutivas: 6, tipoAcompanhamento: 'Individual', telefone: '(81) 98115-7720',
      profissional: 'João Ferreira',
    },
  ];

  // TODO RF21: substituir mock de frequência pela API de relatório de grupos.
  readonly sessoesFrequencia: SessaoFrequenciaMock[] = [
    { id: 1, grupo: 'Grupo de Convivência', data: '05/06/2026', profissionalResponsavel: 'João Ferreira', presentes: ['Ana B. Souza', 'Cleide Ferreira', 'Rafael Torres', 'Lúcia Almeida', 'Pedro Ramos', 'Bruna Alves', 'Caio Lima', 'Sara Melo'], ausentes: ['Marcos V. Lima', 'João P. Nogueira'] },
    { id: 2, grupo: 'Grupo de Convivência', data: '19/06/2026', profissionalResponsavel: 'Ana Oliveira', presentes: ['Ana B. Souza', 'Cleide Ferreira', 'Lúcia Almeida', 'Pedro Ramos', 'Bruna Alves', 'Sara Melo'], ausentes: ['Rafael Torres', 'Marcos V. Lima', 'Caio Lima', 'João P. Nogueira'] },
    { id: 3, grupo: 'Grupo de Cuidado em Álcool e Drogas', data: '10/07/2026', profissionalResponsavel: 'Dra. Marina Costa', presentes: ['Rafael Torres', 'João P. Nogueira', 'Caio Lima', 'Pedro Ramos', 'Bruna Alves', 'Sara Melo', 'Lúcia Almeida', 'Ana B. Souza', 'Marcos V. Lima'], ausentes: ['Cleide Ferreira'] },
    { id: 4, grupo: 'Grupo de Cuidado em Álcool e Drogas', data: '24/07/2026', profissionalResponsavel: 'Dra. Marina Costa', presentes: ['João P. Nogueira', 'Pedro Ramos', 'Caio Lima', 'Bruna Alves', 'Sara Melo'], ausentes: ['Rafael Torres', 'Cleide Ferreira', 'Marcos V. Lima', 'Ana B. Souza', 'Lúcia Almeida'] },
    { id: 5, grupo: 'Grupo de Familiares', data: '07/08/2026', profissionalResponsavel: 'Ana Oliveira', presentes: ['Helena Lima', 'Marta Souza', 'Paulo Ramos', 'Célia Torres', 'Alice Melo', 'Mauro Alves', 'Sônia Nunes'], ausentes: ['Ricardo Lima'] },
    { id: 6, grupo: 'Grupo de Familiares', data: '21/08/2026', profissionalResponsavel: 'João Ferreira', presentes: ['Helena Lima', 'Marta Souza', 'Célia Torres', 'Alice Melo', 'Mauro Alves'], ausentes: ['Paulo Ramos', 'Sônia Nunes', 'Ricardo Lima'] },
  ];

  get pacientesFiltrados(): PacienteBuscaAtivaMock[] {
    return this.pacientesBuscaAtiva.filter((paciente) => {
      const profissionalCorresponde = this.profissionalSelecionado === 'Todos os profissionais'
        || paciente.profissional === this.profissionalSelecionado;
      const tipoCorresponde = this.tipoAcompanhamentoSelecionado === 'Individual e grupo'
        || paciente.tipoAcompanhamento === this.tipoAcompanhamentoSelecionado;
      return profissionalCorresponde && tipoCorresponde;
    });
  }

  get sessoesFiltradas(): SessaoFrequenciaMock[] {
    return this.sessoesFrequencia.filter((sessao) =>
      this.grupoSelecionado === 'Todos os grupos' || sessao.grupo === this.grupoSelecionado,
    );
  }

  get mediaPresenca(): number {
    if (this.sessoesFiltradas.length === 0) return 0;
    const total = this.sessoesFiltradas.reduce((soma, sessao) => soma + this.calcularTaxa(sessao), 0);
    return total / this.sessoesFiltradas.length;
  }

  selecionarAba(aba: TipoRelatorio): void {
    this.abaAtiva = aba;
  }

  selecionarFormato(formato: FormatoExportacao): void {
    // TODO RF21: integrar geração real de PDF, Excel e CSV na próxima etapa.
    this.formatoSelecionado = formato;
  }

  calcularTaxa(sessao: SessaoFrequenciaMock): number {
    const total = sessao.presentes.length + sessao.ausentes.length;
    return total === 0 ? 0 : (sessao.presentes.length / total) * 100;
  }

  formatarPercentual(valor: number): string {
    return valor.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  }

  resumirNomes(nomes: string[]): string {
    if (nomes.length <= 3) return nomes.join(', ');
    return `${nomes.slice(0, 3).join(', ')}, +${nomes.length - 3}`;
  }
}
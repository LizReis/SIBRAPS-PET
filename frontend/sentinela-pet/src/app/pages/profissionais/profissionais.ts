import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  ProfissionalPayload,
  ProfissionalService,
} from '../../services/profissional/profissional-service';

@Component({
  selector: 'app-profissionais',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profissionais.html',
  styleUrl: './profissionais.css',
})
export class Profissionais implements OnInit {
  termoPesquisa = '';
  tipoUsuarioSelecionado = '';

  tipoUsuarioOptions = ['ADMINISTRADOR', 'PROFISSIONAL', 'RECEPCAO'];

  profissionais: ProfissionalPayload[] = [];

  carregando = false;
  erro: string | null = null;

  constructor(
    private router: Router,
    private profissionalService: ProfissionalService,
  ) {}

  ngOnInit(): void {
    this.carregarProfissionais();
  }

  carregarProfissionais(): void {
    this.carregando = true;
    this.erro = null;

    this.profissionalService.listar().subscribe({
      next: (profissionais) => {
        this.profissionais = profissionais;
        this.carregando = false;
      },
      error: (erro) => {
        console.error('Erro ao carregar profissionais', erro);
        this.erro = 'Não foi possível carregar os profissionais.';
        this.carregando = false;
      },
    });
  }

  get profissionaisFiltrados(): ProfissionalPayload[] {
    return this.profissionais.filter((profissional) => {
      const nomeOk = profissional.nome.toLowerCase().includes(this.termoPesquisa.toLowerCase());

      const tipoOk =
        this.tipoUsuarioSelecionado === '' ||
        profissional.tipoUsuario === this.tipoUsuarioSelecionado;

      return nomeOk && tipoOk;
    });
  }

  cadastrarNovoProfissional(): void {
    this.router.navigate(['/profissionais/novo']);
  }

  verDetalhes(idPublico: string): void {
    this.router.navigate(['/profissionais/detalhes', idPublico]);
  }

  editarProfissional(idPublico: string): void {
    this.router.navigate(['/profissionais/editar', idPublico]);
  }

  labelEnum(valor: string): string {
    return valor
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }
}

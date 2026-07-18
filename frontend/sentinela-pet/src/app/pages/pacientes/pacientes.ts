import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { PacientePayload, PacienteService } from '../../services/paciente/paciente-service';

@Component({
  selector: 'app-pacientes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pacientes.html',
  styleUrl: './pacientes.css',
})
export class Pacientes implements OnInit {
  termoPesquisa = '';

  pacientes: PacientePayload[] = [];

  carregando = false;

  constructor(
    private router: Router,
    private pacienteService: PacienteService,
  ) {}

  ngOnInit(): void {
    this.carregarPacientes();
  }

  carregarPacientes(): void {
    this.carregando = true;

    this.pacienteService.listar().subscribe({
      next: (pacientes) => {
        this.pacientes = pacientes;
        this.carregando = false;
      },

      error: (erro) => {
        console.error('Erro ao carregar pacientes', erro);
        this.carregando = false;
      },
    });
  }

  pesquisar(): void {
    const termo = this.termoPesquisa.trim();

    if (!termo) {
      this.carregarPacientes();
      return;
    }

    const somenteNumeros = termo.replace(/\D/g, '');

    if (/^\d+$/.test(termo)) {
      if (somenteNumeros.length === 15) {
        this.pacienteService.buscarPorCns(somenteNumeros).subscribe({
          next: (paciente) => (this.pacientes = [paciente]),

          error: () => (this.pacientes = []),
        });
      } else if (somenteNumeros.length === 11) {
        this.pacienteService.buscarPorCpf(somenteNumeros).subscribe({
          next: (paciente) => (this.pacientes = [paciente]),

          error: () => (this.pacientes = []),
        });
      }

      return;
    }

    this.pacienteService.buscarPorNome(termo).subscribe({
      next: (pacientes) => (this.pacientes = pacientes),

      error: () => (this.pacientes = []),
    });
  }

  cadastrarNovoPaciente(): void {
    this.router.navigate(['/pacientes/novo']);
  }

  verDetalhes(idPublico: string): void {
    this.router.navigate(['/pacientes/detalhes', idPublico]);
  }

  editarPaciente(idPublico: string): void {
    this.router.navigate(['/pacientes/editar', idPublico]);
  }

  formatarCpf(cpf: string): string {
    if (!cpf) return '';

    const numeros = cpf.replace(/\D/g, '');

    return numeros.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  }
}

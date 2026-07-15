import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { PacientePayload, PacienteService } from '../../services/paciente/paciente-service';

@Component({
  selector: 'app-detalhe-paciente',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './detalhe-paciente.html',
  styleUrl: './detalhe-paciente.css',
})
export class DetalhePaciente implements OnInit {
  paciente: PacientePayload | null = null;

  carregando = false;
  erro: string | null = null;

  inativando = false;
  confirmandoInativacao = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private pacienteService: PacienteService,
  ) {}

  ngOnInit(): void {
    const idPublico = this.route.snapshot.paramMap.get('id');

    if (!idPublico) {
      this.erro = 'Paciente não informado.';
      return;
    }

    this.carregarPaciente(idPublico);
  }

  carregarPaciente(idPublico: string): void {
    this.carregando = true;
    this.erro = null;

    this.pacienteService.buscarPorId(idPublico).subscribe({
      next: (paciente) => {
        this.paciente = paciente;
        this.carregando = false;
      },
      error: (erro) => {
        console.error('Erro ao carregar paciente', erro);
        this.erro = 'Não foi possível carregar os dados do paciente.';
        this.carregando = false;
      },
    });
  }

  voltar(): void {
    this.router.navigate(['/pacientes']);
  }

  editarPaciente(): void {
    if (!this.paciente?.idPublico) return;
    this.router.navigate(['/pacientes/editar', this.paciente.idPublico]);
  }

  pedirConfirmacaoInativacao(): void {
    this.confirmandoInativacao = true;
  }

  cancelarInativacao(): void {
    this.confirmandoInativacao = false;
  }

  confirmarInativacao(): void {
    if (!this.paciente?.idPublico) return;

    this.inativando = true;

    this.pacienteService.inativarPaciente(this.paciente.idPublico).subscribe({
      next: () => {
        this.inativando = false;
        this.confirmandoInativacao = false;

        if (this.paciente) {
          this.paciente.statusPaciente = 'INATIVO';
        }
      },
      error: (erro) => {
        console.error('Erro ao inativar paciente', erro);
        this.inativando = false;
        this.erro = 'Não foi possível inativar o paciente. Tente novamente.';
      },
    });
  }

  get pacienteAtivo(): boolean {
    return this.paciente?.statusPaciente !== 'INATIVO';
  }

  formatarCpf(cpf: string | undefined): string {
    if (!cpf) return '-';
    const numeros = cpf.replace(/\D/g, '');
    return numeros.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  }

  formatarTelefone(telefone: string | undefined): string {
    if (!telefone) return '-';
    const numeros = telefone.replace(/\D/g, '');

    if (numeros.length <= 10) {
      return numeros.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
    }
    return numeros.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
  }

  formatarCep(cep: string | undefined): string {
    if (!cep) return '-';
    const numeros = cep.replace(/\D/g, '');
    return numeros.replace(/(\d{5})(\d{3})/, '$1-$2');
  }

  formatarData(data: string | undefined): string {
    if (!data) return '-';
    const [ano, mes, dia] = data.split('-');
    return `${dia}/${mes}/${ano}`;
  }

  labelEnum(valor: string | undefined): string {
    if (!valor) return '-';
    return valor
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (letra) => letra.toUpperCase());
  }
}

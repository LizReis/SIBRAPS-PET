import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { UsuarioLogadoService } from '../../services/usuario-logado-service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterModule, CommonModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  constructor(
    private router: Router,
    private usuarioLogadoService: UsuarioLogadoService,
  ) {}

  menuItems = [
    { label: 'Início', route: '/inicio', icon: 'home' },
    { label: 'Painel', route: '/painel', icon: 'bar_chart' },
    { label: 'Agenda', route: '/agenda', icon: 'calendar_today' },
    { label: 'Pacientes', route: '/pacientes', icon: 'groups' },
    { label: 'Profissionais', route: '/profissionais', icon: 'person' },
  ];

  logout() {
    const confirmado = confirm('Deseja realmente sair?');
    if (confirmado) {
      localStorage.removeItem('token');
      this.usuarioLogadoService.limparCache();
      this.router.navigate(['/login']);
    }
  }
}

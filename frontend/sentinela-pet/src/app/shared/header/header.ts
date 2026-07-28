import { Component, OnInit, inject } from '@angular/core';
import { take } from 'rxjs';
import { UsuarioLogadoDTO, UsuarioLogadoService } from '../../services/usuario-logado-service';

@Component({
  selector: 'app-header',
  imports: [],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit {
  private usuarioLogadoService = inject(UsuarioLogadoService);

  userName = 'Carregando...';
  userInitials = '';
  avatarColor = '';

  // Paleta de cores para o avatar
  private colors = [
    '#005bf0',
    '#004ecc',
    '#003fa3',
    '#002f7a',
    '#001f52',
    '#1d4ed8',
    '#2563eb',
    '#3b82f6',
  ];

  ngOnInit(): void {
    this.usuarioLogadoService
      .obterUsuarioLogado()
      .pipe(take(1))
      .subscribe({
        next: (usuario: UsuarioLogadoDTO) => {
          this.userName = usuario.nome;
          this.userInitials = this.getInitials(usuario.nome);
          this.avatarColor = this.getColor(usuario.nome);
        },
        error: (erro) => {
          console.error('Erro ao carregar usuário logado:', erro);

          this.userName = 'Usuário';
          this.userInitials = this.getInitials(this.userName);
          this.avatarColor = this.getColor(this.userName);
        },
      });
  }

  private getInitials(name: string): string {
    return name
      .split(' ')
      .filter((n) => n.length > 0)
      .slice(0, 2)
      .map((n) => n[0].toUpperCase())
      .join('');
  }

  private getColor(name: string): string {
    const index = name.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0) % this.colors.length;

    return this.colors[index];
  }
}

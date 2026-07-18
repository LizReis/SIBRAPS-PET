import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-header',
  imports: [],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit {
  userName = 'Nome do Usuário'; // futuramente virá do serviço de auth
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

  ngOnInit() {
    this.userInitials = this.getInitials(this.userName);
    this.avatarColor = this.getColor(this.userName);
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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ProfissionalService } from '../../services/profissional/profissional-service';
import { UsuarioLogadoService } from '../../services/usuario-logado-service';

import { Profissionais } from './profissionais';

describe('Profissionais', () => {
  let component: Profissionais;
  let fixture: ComponentFixture<Profissionais>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profissionais],
      providers: [
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: UsuarioLogadoService, useValue: { obterUsuarioLogado: () => of({ tipoUsuario: 'ADMINISTRADOR' }) } },
        { provide: ProfissionalService, useValue: { listar: () => of([
          { idPublico: 'teste', nome: 'Profissional teste', email: 'teste@test.local', tipoUsuario: 'PROFISSIONAL',
            unidadeAtuacao: 'USF', usfAtuacaoId: 7, usfAtuacao: { id: 7, cnes: '7', nomeUsf: 'USF nome da listagem' } },
        ]) } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(Profissionais);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('mostra o nome da USF no mesmo campo da listagem', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('USF nome da listagem');
  });
});

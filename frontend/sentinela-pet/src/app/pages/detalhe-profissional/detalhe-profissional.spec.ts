import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { ProfissionalPayload, ProfissionalService } from '../../services/profissional/profissional-service';
import { UsuarioLogadoService } from '../../services/usuario-logado-service';

import { DetalheProfissional } from './detalhe-profissional';

describe('DetalheProfissional', () => {
  let component: DetalheProfissional;
  let fixture: ComponentFixture<DetalheProfissional>;
  const usuario: ProfissionalPayload = {
    idPublico: 'teste', nome: 'Profissional', email: 'teste@test.local', senha: null,
    tipoUsuario: 'PROFISSIONAL', unidadeAtuacao: 'USF', usfAtuacaoId: 7,
    usfAtuacao: { id: 7, cnes: '7', nomeUsf: 'USF nome recebido do backend' },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalheProfissional],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'teste' } } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ProfissionalService, useValue: { buscarPorId: () => of(usuario) } },
        { provide: UsuarioLogadoService, useValue: { obterUsuarioLogado: () => of({ tipoUsuario: 'ADMINISTRADOR' }) } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetalheProfissional);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('mostra a USF específica nos detalhes existentes', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(usuario.usfAtuacao!.nomeUsf);
  });

  it.each(['CAPS_AD', 'CAPS_I', 'CAPS_II'])('mantém o rótulo do %s', unidade => {
    component.profissional = { ...usuario, unidadeAtuacao: unidade, usfAtuacaoId: null, usfAtuacao: null };
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(component.labelEnum(unidade));
    expect(fixture.nativeElement.textContent).not.toContain(usuario.usfAtuacao!.nomeUsf);
  });
});

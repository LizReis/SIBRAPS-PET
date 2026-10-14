import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { ProfissionalPayload, ProfissionalService } from '../../services/profissional/profissional-service';
import { UnidadeSaude, UnidadeSaudeService } from '../../services/unidade-saude-service';

import { CadastroProfissional } from './cadastro-profissional';

describe('CadastroProfissional', () => {
  let component: CadastroProfissional;
  let fixture: ComponentFixture<CadastroProfissional>;
  let id: string | null;
  const usfs = [
    { id: 7, cnes: '7', nomeUsf: 'USF vinda da API A' },
    { id: 9, cnes: '9', nomeUsf: 'USF vinda da API B' },
  ] as UnidadeSaude[];
  const profissional: ProfissionalPayload = {
    nome: 'Teste', email: 'teste@test.local', tipoUsuario: 'PROFISSIONAL',
    unidadeAtuacao: 'USF', usfAtuacaoId: 7, senha: null,
  };
  const listarUnidades = vi.fn(() => of(usfs));
  const cadastrar = vi.fn(() => of(profissional));
  const atualizar = vi.fn(() => of(profissional));
  const buscarPorId = vi.fn(() => of(profissional));

  beforeEach(async () => {
    id = null;
    vi.clearAllMocks();
    listarUnidades.mockReturnValue(of(usfs));
    buscarPorId.mockReturnValue(of(profissional));
    await TestBed.configureTestingModule({
      imports: [CadastroProfissional],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => id } } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ProfissionalService, useValue: { cadastrar, atualizar, buscarPorId } },
        { provide: UnidadeSaudeService, useValue: { listarUnidades } },
      ],
    })
    .compileComponents();

  });

  function criar() {
    fixture = TestBed.createComponent(CadastroProfissional);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function preencher(unidade: string) {
    component.profissionalForm.patchValue({
      nome: 'Teste', email: 'teste@test.local', tipoUsuario: 'PROFISSIONAL',
      senha: 'senha-teste', confirmarSenha: 'senha-teste', unidadeAtuacao: unidade,
    });
  }

  it('should create', () => {
    criar();
    expect(component).toBeTruthy();
  });

  it('mantém um select com os três CAPS e USFs da API, sem USF genérica', () => {
    criar();
    const options = Array.from(fixture.nativeElement.querySelectorAll('#unidadeAtuacao option')) as HTMLOptionElement[];
    expect(options.map(o => o.textContent?.trim())).toEqual([
      'Selecione a unidade', 'CAPS AD', 'CAPS I', 'CAPS II', usfs[0].nomeUsf, usfs[1].nomeUsf,
    ]);
    expect(options.some(o => o.value === 'USF')).toBe(false);
    expect(listarUnidades).toHaveBeenCalledOnce();
  });

  it.each(['CAPS_AD', 'CAPS_I', 'CAPS_II'])('envia %s sem associação USF', unidade => {
    criar(); preencher(unidade); component.salvarProfissional();
    expect(cadastrar).toHaveBeenCalledWith(expect.objectContaining({ unidadeAtuacao: unidade, usfAtuacaoId: null }));
  });

  it('envia USF com o ID persistido', () => {
    criar(); preencher('USF:7'); component.salvarProfissional();
    expect(cadastrar).toHaveBeenCalledWith(expect.objectContaining({ unidadeAtuacao: 'USF', usfAtuacaoId: 7 }));
  });

  it('pré-seleciona a USF quando a lista chega depois do usuário', () => {
    id = 'usuario-id';
    const resposta = new Subject<UnidadeSaude[]>();
    listarUnidades.mockReturnValue(resposta);
    criar();
    expect(component.profissionalForm.controls.unidadeAtuacao.value).toBe('USF:7');
    resposta.next(usfs); fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('#unidadeAtuacao') as HTMLSelectElement).value).toBe('USF:7');
  });

  it.each([
    ['USF', 7, 'USF:9', 'USF', 9],
    ['USF', 7, 'CAPS_AD', 'CAPS_AD', null],
    ['CAPS_I', null, 'USF:7', 'USF', 7],
  ])('edita %s para %s sem senha obrigatória', (origem, usfId, selecao, destino, esperado) => {
    id = 'usuario-id';
    buscarPorId.mockReturnValue(of({ ...profissional, unidadeAtuacao: origem as string, usfAtuacaoId: usfId as number | null }));
    criar();
    component.profissionalForm.controls.unidadeAtuacao.setValue(selecao as string);
    component.salvarProfissional();
    expect(atualizar).toHaveBeenCalledWith(id, expect.objectContaining({ unidadeAtuacao: destino, usfAtuacaoId: esperado, senha: null }));
  });

  it('USF legada precisa de uma unidade específica antes de salvar', () => {
    id = 'usuario-id';
    buscarPorId.mockReturnValue(of({ ...profissional, usfAtuacaoId: null }));
    criar();
    expect(component.profissionalForm.controls.unidadeAtuacao.value).toBeNull();
    component.salvarProfissional();
    expect(atualizar).not.toHaveBeenCalled();
  });
});

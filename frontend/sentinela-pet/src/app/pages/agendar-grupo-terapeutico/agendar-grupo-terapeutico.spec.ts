import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { of } from 'rxjs';
import { AgendarGrupoTerapeutico } from './agendar-grupo-terapeutico';

describe('AgendarGrupoTerapeutico - seleção assistencial', () => {
  it('aceita administrador e profissional da lista como coordenadores', () => {
    TestBed.configureTestingModule({ providers: [FormBuilder] });
    const lista = [
      { idPublico: 'admin', nome: 'Admin assistencial', tipoUsuario: 'ADMINISTRADOR' },
      { idPublico: 'prof', nome: 'Profissional assistencial', tipoUsuario: 'PROFISSIONAL' },
    ];
    const servico = { listarParaSelecao: vi.fn(() => of(lista)) };
    const component = TestBed.runInInjectionContext(() => new AgendarGrupoTerapeutico(
      {} as any, {} as any, servico as any, {} as any,
      { snapshot: { paramMap: { get: () => null } } } as any,
    ));
    component.ngOnInit();
    expect(servico.listarParaSelecao).toHaveBeenCalledOnce();
    expect(component.profissionais.map(p => p.idPublico)).toEqual(['admin', 'prof']);
    for (const pessoa of lista) {
      component.form.controls.coordenadorId.setValue(pessoa.idPublico);
      expect(component.coordenadorNome).toBe(pessoa.nome);
    }
    component.ngOnDestroy();
  });
});

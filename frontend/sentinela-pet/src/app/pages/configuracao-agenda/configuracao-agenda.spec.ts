import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { ConfiguracaoAgenda } from './configuracao-agenda';



describe('ConfiguracaoAgenda - integração dos modais', () => {
  let component: ConfiguracaoAgenda;
  let disponibilidade: any;
  let bloqueio: any;
  let excecao: any;

  beforeEach(() => {
    disponibilidade = { listar: vi.fn(() => of([])), salvar: vi.fn(() => of({})), remover: vi.fn(() => of(void 0)) };
    bloqueio = { listar: vi.fn(() => of([])), salvar: vi.fn(() => of({})), remover: vi.fn(() => of(void 0)) };
    excecao = { listar: vi.fn(() => of([])), salvar: vi.fn(() => of({})), remover: vi.fn(() => of(void 0)) };
    component = new ConfiguracaoAgenda(
      { obterUsuarioLogado: vi.fn() } as any,
      { listar: vi.fn() } as any,
      disponibilidade,
      bloqueio,
      excecao,
    );
    component.isAdmin = true;
    component.profissionalSelecionadoId = '61e45a3c-f6df-4f54-8c56-6b46f874e092';
  });

  it.each([
    ['horario', 'salvarHorario'], ['bloqueio', 'salvarBloqueio'],
    ['excecao', 'salvarExcecao'], ['confirmacao', 'excluir'],
  ] as const)('encaminha o modal %s ao método %s', (modal, metodo) => {
    component.modal = modal;
    const spy = vi.spyOn(component, metodo).mockImplementation(() => undefined);
    component.confirmarModal();
    expect(spy).toHaveBeenCalledOnce();
  });

  it('envia horário com idPublico do profissional e capacidade numérica', () => {
    component.modal = 'horario';
    component.disponibilidadeForm = { diaSemana: 'MONDAY', turno: 'MANHA', capacidade: '5' as any };
    component.salvarHorario();
    expect(disponibilidade.salvar).toHaveBeenCalledWith({
      diaSemana: 'MONDAY', turno: 'MANHA', capacidade: 5,
      usuarioId: '61e45a3c-f6df-4f54-8c56-6b46f874e092',
    });
    expect(component.modal).toBeNull();
    expect(disponibilidade.listar).toHaveBeenCalled();
  });

  it('omite usuarioId ao salvar como profissional', () => {
    component.isAdmin = false; component.isProfissional = true; component.profissionalSelecionadoId = null;
    component.disponibilidadeForm = { diaSemana: 'TUESDAY', turno: 'TARDE', capacidade: 2 };
    component.salvarHorario();
    expect(disponibilidade.salvar.mock.calls[0][0].usuarioId).toBeUndefined();
  });

  it('não envia horário de administrador sem profissional selecionado', () => {
    component.profissionalSelecionadoId = null;
    component.disponibilidadeForm = { diaSemana: 'MONDAY', turno: 'MANHA', capacidade: 5 };
    component.salvarHorario();
    expect(disponibilidade.salvar).not.toHaveBeenCalled();
    expect(component.erroModal).toContain('Selecione um profissional');
  });

  it('envia datas ISO e motivo obrigatório no bloqueio', () => {
    component.bloqueioForm = { dataInicio: '2026-08-10', dataFim: '2026-08-15', motivoBloqueio: ' Férias ' };
    component.salvarBloqueio();
    expect(bloqueio.salvar).toHaveBeenCalledWith(expect.objectContaining({
      dataInicio: '2026-08-10', dataFim: '2026-08-15', motivoBloqueio: 'Férias',
    }));
  });

  it('não envia bloqueio com intervalo inválido', () => {
    component.bloqueioForm = { dataInicio: '2026-08-15', dataFim: '2026-08-10', motivoBloqueio: 'Férias' };
    component.salvarBloqueio();
    expect(bloqueio.salvar).not.toHaveBeenCalled();
  });

  it('aceita capacidade zero na exceção e envia number', () => {
    component.excecaoForm = { data: '2026-08-20', turno: 'TARDE', capacidade: '0' as any };
    component.salvarExcecao();
    expect(excecao.salvar).toHaveBeenCalledWith(expect.objectContaining({ capacidade: 0 }));
  });

  it('rejeita capacidade negativa na exceção', () => {
    component.excecaoForm = { data: '2026-08-20', turno: 'TARDE', capacidade: -1 };
    component.salvarExcecao();
    expect(excecao.salvar).not.toHaveBeenCalled();
  });

  it('mantém modal e campos quando backend retorna conflito', () => {
    disponibilidade.salvar.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { message: 'Já existe disponibilidade cadastrada.' },
    })));
    component.modal = 'horario';
    component.disponibilidadeForm = { diaSemana: 'MONDAY', turno: 'MANHA', capacidade: 5 };
    component.salvarHorario();
    expect(component.modal).toBe('horario');
    expect(component.disponibilidadeForm.capacidade).toBe(5);
    expect(component.erroModal).toBe('Já existe disponibilidade cadastrada.');
  });

  it('impede duplo envio enquanto a primeira requisição está pendente', () => {
    const resposta = new Subject(); disponibilidade.salvar.mockReturnValue(resposta);
    component.disponibilidadeForm = { diaSemana: 'MONDAY', turno: 'MANHA', capacidade: 5 };
    component.salvarHorario(); component.salvarHorario();
    expect(disponibilidade.salvar).toHaveBeenCalledOnce();
    expect(component.salvando).toBe(true);
  });


  it('impede duplo envio enquanto a primeira requisição está pendente', () => {
    const resposta = new Subject(); disponibilidade.salvar.mockReturnValue(resposta);
    component.disponibilidadeForm = { diaSemana: 'MONDAY', turno: 'MANHA', capacidade: 5 };
    component.salvarHorario(); component.salvarHorario();
    expect(disponibilidade.salvar).toHaveBeenCalledOnce();
    expect(component.salvando).toBe(true);
  });
});

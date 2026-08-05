import { of } from 'rxjs';
import { Agenda } from './agenda';
import { AgendamentoDTO } from '../../services/agendamento-service';

const base: AgendamentoDTO = {
  id: 1,
  usuarioId: 'prof-1',
  nomeProfissional: 'João Ferreira',
  pacienteId: 'pac-1',
  nomePaciente: 'Lizandra Reis com nome suficientemente longo para teste',
  tipoAcompanhamento: 'INDIVIDUAL',
  dataAgendamento: '2026-08-05',
  turnoAgendamento: 'TARDE',
  horaAtendimento: '15:45:00',
  situacaoAtendimento: 'AGENDADO',
  version: 1,
};

function criarComponente(): Agenda {
  const usuarioService = { obterUsuarioLogado: () => of({ idPublico: 'u', nome: 'Admin', email: 'a@a.com', tipoUsuario: 'ADMINISTRADOR', unidadeAtuacao: 'CAPS' }) };
  const profissionalService = { listar: () => of([]) };
  const agendamentoService = { buscarAgendaPorPeriodo: () => of([]), atualizarStatus: () => of(base) };
  const router = { navigate: () => Promise.resolve(true) };
  return new Agenda(usuarioService as any, profissionalService as any, agendamentoService as any, router as any);
}

describe('Agenda', () => {
  it('calcula a posição por minuto para atendimento às 15:45 sem arredondar', () => {
    const component = criarComponente();
    component.dataSelecionada = '2026-08-05';
    component.agendamentos = [base];

    expect(component.formatarHora(base.horaAtendimento)).toBe('15:45');
    expect(parseFloat(component.estiloAgendamentoDia(base)['top'])).toBeCloseTo(1023);
  });

  it('mantém atendimentos com minutos diferentes de zero na agenda do dia', () => {
    const component = criarComponente();
    component.dataSelecionada = '2026-08-05';
    component.agendamentos = [
      { ...base, id: 1, horaAtendimento: '08:05:00' },
      { ...base, id: 2, horaAtendimento: '10:30:00' },
      { ...base, id: 3, horaAtendimento: '15:45:00' },
    ];

    expect(component.agendamentosDia.map((a) => component.formatarHora(a.horaAtendimento))).toEqual(['08:05', '10:30', '15:45']);
  });

  it('exibe REMARCADO_ORIGEM como AGENDAMENTO ANTERIOR sem alterar o valor da API', () => {
    const component = criarComponente();

    expect(component.labelSituacao('REMARCADO_ORIGEM')).toBe('AGENDAMENTO ANTERIOR');
    expect('REMARCADO_ORIGEM').toBe('REMARCADO_ORIGEM');
  });

  it('controla exibição das ações principais no card', () => {
    const component = criarComponente();
    component.podeCriarAgendamento = true;
    component.dataSelecionada = '2026-08-05';

    expect(component.podeRegistrarFrequencia({ ...base, dataAgendamento: '2020-01-01' })).toBe(true);
    expect(component.podeRemarcar({ ...base, tipoAcompanhamento: 'INDIVIDUAL' })).toBe(true);
    expect(component.podeRemarcar({ ...base, situacaoAtendimento: 'CANCELADO' })).toBe(false);
    expect(component.podeRemarcar({ ...base, tipoAcompanhamento: 'GRUPO_TERAPEUTICO' })).toBe(false);
  });
});

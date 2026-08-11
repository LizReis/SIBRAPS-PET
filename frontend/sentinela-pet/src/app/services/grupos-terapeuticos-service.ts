import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type RecorrenciaGrupo = 'UNICA' | 'SEMANAL' | 'QUINZENAL' | 'MENSAL';
export type StatusSessaoGrupo = 'AGENDADA' | 'REALIZADA' | 'CANCELADA';
export type StatusVisualSessao = StatusSessaoGrupo | 'EM_ANDAMENTO';

export interface ParticipanteSessaoPayload {
  pacienteId: string;
  nomePaciente: string;
}

export interface SessaoGrupoPayload {
  id: number;
  grupoId: number;
  temaGrupo: string;
  nomeCoordenador: string;
  dataSessao: string;
  horario: string;
  status: StatusSessaoGrupo;
  participantes: ParticipanteSessaoPayload[];
  quantidadeParticipantes: number;
  version: number;
}

export interface GrupoTerapeuticoPayload {
  id: number;
  tema: string;
  coordenadorId: string;
  nomeCoordenador: string;
  recorrencia: RecorrenciaGrupo;
  horarioPadrao: string;
  ativo: boolean;
}

export interface CriarGrupoPayload {
  tema: string;
  coordenadorId: string;
  recorrencia: RecorrenciaGrupo;
  dataPrimeiraSessao: string;
  horario: string;
  participantesIds: string[];
}

@Injectable({ providedIn: 'root' })
export class GrupoTerapeuticoService {
  private readonly apiUrl = '/api/grupos';
  constructor(private readonly http: HttpClient) {}

  criar(payload: CriarGrupoPayload): Observable<GrupoTerapeuticoPayload> {
    return this.http.post<GrupoTerapeuticoPayload>(this.apiUrl, payload);
  }
  listar(): Observable<GrupoTerapeuticoPayload[]> {
    return this.http.get<GrupoTerapeuticoPayload[]>(this.apiUrl);
  }
  listarSessoes(dataInicio: string, dataFim: string): Observable<SessaoGrupoPayload[]> {
    const params = new HttpParams().set('dataInicio', dataInicio).set('dataFim', dataFim);
    return this.http.get<SessaoGrupoPayload[]>(`${this.apiUrl}/sessoes`, { params });
  }
  adicionarParticipante(sessaoId: number, pacienteId: string): Observable<SessaoGrupoPayload> {
    return this.http.post<SessaoGrupoPayload>(`${this.apiUrl}/sessoes/${sessaoId}/participantes`, { pacienteId });
  }
  removerParticipante(sessaoId: number, pacienteId: string): Observable<SessaoGrupoPayload> {
    return this.http.delete<SessaoGrupoPayload>(`${this.apiUrl}/sessoes/${sessaoId}/participantes/${pacienteId}`);
  }
  atualizarStatus(sessao: SessaoGrupoPayload, novoStatus: StatusSessaoGrupo): Observable<SessaoGrupoPayload> {
    const params = new HttpParams().set('novoStatus', novoStatus).set('version', sessao.version);
    return this.http.patch<SessaoGrupoPayload>(`${this.apiUrl}/sessoes/${sessao.id}/status`, null, { params });
  }
}
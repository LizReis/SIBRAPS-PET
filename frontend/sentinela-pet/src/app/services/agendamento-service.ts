import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AgendamentoDTO {
  id: number;
  usuarioId: string;
  nomeProfissional: string;
  pacienteId: string;
  nomePaciente: string;
  tipoAcompanhamento: string;
  dataAgendamento: string;
  turnoAgendamento: string;
  horaAtendimento: string;
  situacaoAtendimento: string;
  agendamentoOriginalId?: number;
  version: number;
}

export interface NovoAgendamentoPayload {
  id?: number;
  usuarioId: string;
  pacienteId: string;
  dataAgendamento: string;
  turnoAgendamento: string;
  horaAtendimento: string;
}

export interface VagasPorTurno {
  MANHA: number;
  TARDE: number;
}

export interface FieldMessage {
  fieldName: string;
  message: string;
}

export interface StandardError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface ValidationError extends StandardError {
  errors: FieldMessage[];
}

@Injectable({
  providedIn: 'root',
})
export class AgendamentoService {
  private readonly apiUrl = 'http://localhost:8080/api/agendamentos';

  constructor(private http: HttpClient) {}

  buscarAgendaDoDia(data: string, profissionalId?: string): Observable<AgendamentoDTO[]> {
    let params = new HttpParams();
    if (profissionalId) {
      params = params.set('profissionalId', profissionalId);
    }
    return this.http.get<AgendamentoDTO[]>(`${this.apiUrl}/agenda/${data}`, { params });
  }

  atualizarStatus(id: number, novoStatus: string, version: number): Observable<AgendamentoDTO> {
    const params = new HttpParams().set('novoStatus', novoStatus).set('version', String(version));

    return this.http.patch<AgendamentoDTO>(`${this.apiUrl}/${id}/status`, null, { params });
  }

  consultarVagas(usuarioId: string, data: string): Observable<VagasPorTurno> {
    const params = new HttpParams().set('usuarioId', usuarioId).set('data', data);
    return this.http.get<VagasPorTurno>(`${this.apiUrl}/vagas`, { params });
  }

  criarAgendamento(payload: NovoAgendamentoPayload): Observable<AgendamentoDTO> {
    return this.http.post<AgendamentoDTO>(this.apiUrl, payload);
  }
}

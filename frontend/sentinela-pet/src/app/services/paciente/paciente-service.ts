import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface EnderecoPayload {
  cidade: string;
  estado: string;
  bairro: string;
  logradouro: string;
  numero: string;
  complemento: string;
  cep: string;
}

export interface UsfReferencia {
  id: number;
  cnes: string;
  nomeUsf: string;
  bairro: string;
  logradouro: string;
  latitude: string;
  longitude: string;
}

export interface PacientePayload {
  idPublico?: string;

  nome: string;
  nomeMae: string;
  dataNascimento: string;

  dataUltimaPresenca?: string;

  sexo: string;
  racacor: string;

  cns: string;
  cpf: string;
  telefone: string;

  endereco: EnderecoPayload;

  situacaoRua: boolean;
  tipoAcompanhamento: string;

  countFaltas?: number;
  statusPaciente?: string;

  usfReferencia: UsfReferencia;
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
export class PacienteService {
  private readonly apiUrl = 'http://localhost:8080/api/pacientes';

  constructor(private http: HttpClient) {}

  listar(): Observable<PacientePayload[]> {
    return this.http.get<PacientePayload[]>(this.apiUrl);
  }

  buscarPorNome(nome: string): Observable<PacientePayload[]> {
    return this.http.get<PacientePayload[]>(
      `${this.apiUrl}/busca/nome?q=${encodeURIComponent(nome)}`,
    );
  }

  buscarPorCpf(cpf: string): Observable<PacientePayload> {
    return this.http.get<PacientePayload>(`${this.apiUrl}/busca/cpf/${cpf}`);
  }

  buscarPorCns(cns: string): Observable<PacientePayload> {
    return this.http.get<PacientePayload>(`${this.apiUrl}/busca/cns/${cns}`);
  }

  cadastrarPaciente(paciente: PacientePayload): Observable<PacientePayload> {
    return this.http.post<PacientePayload>(this.apiUrl, paciente);
  }

  atualizarPaciente(idPublico: string, paciente: PacientePayload): Observable<PacientePayload> {
    return this.http.put<PacientePayload>(`${this.apiUrl}/${idPublico}`, paciente);
  }

  buscarPorId(idPublico: string): Observable<PacientePayload> {
    return this.http.get<PacientePayload>(`${this.apiUrl}/${idPublico}`);
  }

  inativarPaciente(idPublico: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${idPublico}`);
  }
}

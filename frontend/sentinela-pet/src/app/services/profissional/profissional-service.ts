import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProfissionalPayload {
  idPublico?: string;

  nome: string;
  email: string;
  senha: string | null;

  tipoUsuario: string;
  unidadeAtuacao: string;
}

export interface ProfissionalSelecao {
  idPublico: string;
  nome: string;
  unidadeAtuacao: string;
  tipoUsuario: 'PROFISSIONAL';
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
export class ProfissionalService {
  private readonly apiUrl = '/api/usuarios';

  constructor(private http: HttpClient) {}

  listar(): Observable<ProfissionalPayload[]> {
    return this.http.get<ProfissionalPayload[]>(this.apiUrl);
  }

  listarParaSelecao(): Observable<ProfissionalPayload[]> {
    return this.http.get<ProfissionalPayload[]>(`${this.apiUrl}/profissionais-selecao`);
  }


  buscarPorId(idPublico: string): Observable<ProfissionalPayload> {
    return this.http.get<ProfissionalPayload>(`${this.apiUrl}/${idPublico}`);
  }

  cadastrar(usuario: ProfissionalPayload): Observable<ProfissionalPayload> {
    return this.http.post<ProfissionalPayload>(this.apiUrl, usuario);
  }

  atualizar(idPublico: string, usuario: ProfissionalPayload): Observable<ProfissionalPayload> {
    return this.http.put<ProfissionalPayload>(`${this.apiUrl}/${idPublico}`, usuario);
  }

  remover(idPublico: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${idPublico}`);
  }
}

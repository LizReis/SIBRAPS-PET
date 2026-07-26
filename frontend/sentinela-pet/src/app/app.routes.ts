import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { MainLayout } from './layouts/main-layout/main-layout';
import { Inicio } from './pages/inicio/inicio';
import { Painel } from './pages/painel/painel';
import { Agenda } from './pages/agenda/agenda';
import { Pacientes } from './pages/pacientes/pacientes';
import { CadastroPaciente } from './pages/cadastro-paciente/cadastro-paciente';
import { loginGuard } from './auth/login-guard';
import { authGuard } from './auth/auth-guard';
import { Profissionais } from './pages/profissionais/profissionais';
import { CadastroProfissional } from './pages/cadastro-profissional/cadastro-profissional';
import { DetalhePaciente } from './pages/detalhe-paciente/detalhe-paciente';
import { DetalheProfissional } from './pages/detalhe-profissional/detalhe-profissional';
import { ConfiguracaoAgenda } from './pages/configuracao-agenda/configuracao-agenda';
import { NovoAgendamento } from './pages/novo-agendamento/novo-agendamento';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  {
    path: '',
    component: AuthLayout,
    children: [{ path: 'login', component: Login, canActivate: [loginGuard] }],
  },

  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: 'inicio', component: Inicio },
      { path: 'painel', component: Painel },
      { path: 'agenda', component: Agenda },
      { path: 'agenda/configuracoes', component: ConfiguracaoAgenda },
      { path: 'agenda/novo', component: NovoAgendamento },
      { path: 'pacientes', component: Pacientes },
      { path: 'pacientes/detalhes/:id', component: DetalhePaciente },
      { path: 'pacientes/novo', component: CadastroPaciente },
      { path: 'pacientes/editar/:id', component: CadastroPaciente },
      { path: 'profissionais', component: Profissionais },
      { path: 'profissionais/novo', component: CadastroProfissional },
      { path: 'profissionais/detalhes/:id', component: DetalheProfissional },
      { path: 'profissionais/editar/:id', component: CadastroProfissional },
    ],
  },
];

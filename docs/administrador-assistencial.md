# Administrador assistencial e cadastro na própria unidade

Revisão baseada em `origin/main` no commit `1f7c368`. O código atual foi analisado antes das alterações, incluindo todas as ocorrências de `TipoUsuario.PROFISSIONAL` no backend.

O administrador não aparecia porque `listarProfissionaisParaSelecao()` filtrava somente PROFISSIONAL. Além disso, `UsuarioContextService` e duas validações de `AgendamentoServiceImpl` rejeitavam ADMINISTRADOR como proprietário/responsável. As regras de autorização e as regras de elegibilidade foram conferidas separadamente.

| Fluxo | Resultado |
| --- | --- |
| `GET /api/usuarios/profissionais-selecao` | Mantém a URL, o DTO e a ordenação por nome. Usa `specificationUsuario()` AND role em ADMINISTRADOR/PROFISSIONAL. RECEPCAO é excluída; administradores e profissionais externos também. O administrador aparece na própria lista. |
| Configuração de Agenda | `determinarUsuarioAlvo()` aceita agenda de ADMINISTRADOR ou PROFISSIONAL. `validarAlteracao()` permite aos gestores já existentes operar recursos desses proprietários. ADMIN pode criar/editar/excluir disponibilidade, bloqueio e exceção próprios. RECEPCAO mantém a gestão permitida, sem poder possuir agenda assistencial. |
| Novo Agendamento | `save()` e `buscarHorariosDisponiveis()` aceitam ADMINISTRADOR/PROFISSIONAL. A validação de unidade permanece antes da elegibilidade. Paciente ativo, datas, conflitos, horários ocupados, bloqueios, capacidade, exceções e remarcação preservados. ADMIN só recebe atendimento com disponibilidade válida. |
| Grupo Terapêutico | Criação e edição já aceitam ADMINISTRADOR/PROFISSIONAL e validam unidade. Nenhuma alteração no serviço de grupos; corrigir a seleção resolve a ausência no campo de coordenador. Testes reais confirmam os dois roles, rejeição de RECEPCAO e de coordenador externo. |
| Novo cadastro | `UsuarioServiceImpl.save()` valida a unidade de destino contra o usuário autenticado obtido de `EscopoUnidadeService`, sem confiar no payload. No formulário, `/me` fornece a única opção, já preenchida e desabilitada. Não consulta todas as USFs para cadastrar. |
| CAPS | O destino precisa ter o mesmo `UnidadeAtuacao` do administrador: AD, I e II são instituições distintas. A associação USF permanece normalizada para null. |
| USF | O destino precisa ser USF e ter exatamente o ID de `usfAtuacao` do administrador. USF Centro não cadastra na USF Andaiá nem em CAPS. Usuário sem USF específica configurada continua bloqueado. |
| Edição | Havia desvio: um usuário próprio podia ser transferido arbitrariamente. Agora a mesma validação de destino retorna 422 antes das alterações. A validação anterior do alvo continua: alvo externo recebe 404. A tela de edição mantém sua lógica de carregamento existente e apresenta a mensagem de 422 se houver tentativa de transferência. |
| Bootstrap | DevConfig usa entidades/repositórios e não foi alterado. Chamadas internas de carga/fixtures sem Authentication mantêm o comportamento existente; o HTTP exige autenticação e ADMIN para salvar usuários. O teste de POST anônimo confirma 401; PROFISSIONAL/RECEPCAO recebem 403. |

`EscopoUnidadeService` permanece intacto e aplicado às seleções, aos alvos e às operações existentes. Não houve substituição de autorização por filtros do frontend. Não foram criados roles ou enums. As ocorrências de PROFISSIONAL referentes à própria agenda e às permissões de frequência foram preservadas.

Os consumidores Angular em Configuração de Agenda, Novo Agendamento, Agenda e Grupo Terapêutico já usam `listarParaSelecao()` e não excluem administradores localmente; não precisaram de alteração em produção. A tipagem `ProfissionalSelecao.tipoUsuario` foi ampliada para os dois roles. `UsuarioLogadoDTO` declara os campos USF já retornados por `/me`; serviço/cache/URL mantidos.

## Relatórios

`RelatorioServiceImpl` já protege as exportações: exige unidade configurada, rejeita profissional/grupo externo e filtra pacientes, grupos/coordenadores e participantes pelo escopo autenticado. “Todos os profissionais” significa todos da instituição autorizada. A tela mantém `referencia-selecao`, que já inclui administradores/profissionais e exclui recepção, e `listarGrupos()`, que já devolve grupos do escopo.

Não foram alterados serviços, DTOs, filtros, templates ou renderizadores de relatórios. A cobertura existente foi reaproveitada para PDF/Excel de CAPS AD, CAPS II e duas USFs específicas; o teste de exportação AD foi ampliado para PROFISSIONAL e RECEPCAO. PDFs e planilhas são efetivamente gerados e lidos, inclusive nas tentativas com IDs externos.

A pré-visualização Angular ainda usa `pacientesBuscaAtiva` e `sessoesFrequencia` mockados. Esses arrays não alimentam as exportações reais. Essa integração não foi ampliada nesta tarefa.

## Arquivos alterados

Backend, sob `backend/buscaativa/src/main/java/com/pet/buscaativa/services/impl/`:

- `UsuarioServiceImpl.java`: seleção assistencial e validação do destino de cadastro/edição.
- `UsuarioContextService.java`: proprietário ADMINISTRADOR em recursos de agenda.
- `AgendamentoServiceImpl.java`: ADMINISTRADOR como responsável por atendimento e consulta de horários.

Frontend, sob `frontend/sentinela-pet/src/app/`:

- `pages/cadastro-profissional/cadastro-profissional.ts`: unidade de /me, única e bloqueada no cadastro.
- `services/usuario-logado-service.ts`: campos USF no DTO TypeScript.
- `services/profissional/profissional-service.ts`: união de roles na interface de seleção.

Testes backend: `ProfissionaisEscopoTests.java`, `UsuarioUnidadeTests.java`, `IsolamentoUnidadeTests.java`. Testes frontend: `cadastro-profissional.spec.ts`, `configuracao-agenda.spec.ts`, `novo-agendamento.spec.ts`, `profissional-service.spec.ts`, e o novo `agendar-grupo-terapeutico.spec.ts`. Documentação: este relatório e aviso de atualização em `isolamento-por-unidade.md`.

Outros arquivos analisados, sem alteração: EscopoUnidadeService; serviços de disponibilidade, bloqueio e exceção; GrupoTerapeuticoServiceImpl; RelatorioServiceImpl; controllers, DTOs, repositories e validador de disponibilidade utilizados nos fluxos; DevConfig; SecurityConfig; telas e serviços Angular consumidores dos selects e relatórios.

## Testes e verificação

- Seleção nos cinco escopos: administrador e profissional presentes, administrador logado incluído, recepção ausente, usuários externos ausentes e ordenação preservada.
- Cadastro HTTP de todos os três roles na instituição correta, CAPS e duas USFs; payloads manuais para qualquer outra instituição rejeitados; senha codificada, email duplicado, permissões e POST anônimo verificados.
- Edição: testes antigos que esperavam transferências foram adaptados para exigir bloqueio e dados intactos; edição de dados na mesma unidade continua testada.
- Agenda: CRUD real de disponibilidade/bloqueio/exceção para ADMIN próprio, outro ADMIN local e PROFISSIONAL; gestão por ADMIN/RECEP preservada; recepção proprietária e usuários externos rejeitados.
- Atendimentos reais: ADMIN/PROF, horários, capacidade, conflito, bloqueio e exceção; paciente e responsável externo rejeitados.
- Grupos: criação/edição para ADMIN/PROF, rejeição de recepção e coordenadores externos, sem tocar nas regras de recorrência/frequência.
- Angular: unidade única, carga assíncrona de /me, falha/USF legada, payload com campo desabilitado, erro de transferência e seleção de administradores nas três telas assistenciais.

Nenhum teste foi desabilitado. Nenhuma regra de negócio sem relação com a tarefa foi alterada: pacientes, classificação, faltas, frequência/recorrência, busca ativa, auditoria, JWT, layouts PDF/Excel, sidebar, header, CSS, dependências e budgets permanecem intactos.

| Verificação executada | Resultado |
| --- | --- |
| Maven Wrapper: `bash ./mvnw -s <settings-do-ambiente> -Dmaven.repo.local=<cache-do-ambiente> -B -ntp clean test` | BUILD SUCCESS: 128 testes, zero falhas, erros ou ignorados. O wrapper existente não é executável; bash preserva seu modo original. |
| Angular: `CI=true npm test -- --watch=false` com `--include` de cadastro-profissional, configuracao-agenda, novo-agendamento, agendar-grupo-terapeutico, profissional-service, relatorios, profissionais e detalhe-profissional | 58 testes passaram nos 8 arquivos relacionados. A suíte Angular completa não foi reexecutada. |
| Angular: `npm run build` | Sucesso. Bundle inicial 846,02 kB; aviso de orçamento de 500 kB, abaixo do limite de erro de 1 MB. Budgets inalterados. |
| Diff completo e `git diff --check` | Revisados; alterações limitadas aos fluxos e testes descritos, sem erros de formatação. |

O teste de contexto mantém o tratamento existente da indisponibilidade da sincronização externa SUS e passou. Nos testes de escopo, somente essa fronteira de rede é substituída por mock; JWT, controllers, services, H2, PDF e Excel executam de verdade.

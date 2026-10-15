# Complemento: escopo de Profissionais

Atualização posterior: [Administrador assistencial e cadastro na própria unidade](administrador-assistencial.md). Essa revisão inclui ADMINISTRADOR nas seleções assistenciais e restringe também o destino do cadastro/edição; substitui as afirmações históricas abaixo sobre exclusividade de PROFISSIONAL na seleção e liberdade de transferência entre instituições.

Esta revisão partiu de `origin/main` em `786dd62`. O relato histórico abaixo descreve a implementação anterior; suas referências à administração global e à correção de USF legada por outro administrador foram substituídas pelas restrições desta revisão.

`GET /api/usuarios` vazava usuários porque `UsuarioServiceImpl.findAll()` chamava `usuarioRepository.findAll()` sem escopo. A consulta agora reutiliza `EscopoUnidadeService.specificationUsuario()` e ordena por nome: CAPS compara a categoria; USF compara a associação específica persistida. ADMINISTRADOR e RECEPCAO, quando autorizados pelo controller, recebem somente usuários da própria instituição.

O acesso por UUID aplica `usuarioPertenceAoEscopo` antes de devolver o DTO. PUT valida a unidade **atual do alvo**, antes de validar o conteúdo recebido ou alterar a entidade; enviar a unidade do administrador no payload não permite capturar um usuário externo. DELETE valida o alvo antes das verificações de vínculo e da remoção. UUID externo e inexistente recebem o mesmo padrão de 404, sem nome ou email do alvo. As regras existentes de cadastro, senha, destino da edição e vínculo como referência permanecem iguais. `/me`, `findByEmail`, JWT e os dois endpoints de seleção não foram modificados. Usuário USF legado sem associação mantém `/me`, mas a listagem falha fechada; um administrador de outra instituição também não pode editar esse legado.

O filtro da listagem oferece “Todas as unidades”, CAPS AD, CAPS I, CAPS II e os nomes reais retornados por `UnidadeSaudeService.listarUnidades()` (`GET /api/unidades-saude`). As opções são tipadas com valor, label, tipo e CNES quando USF. Não há opção genérica “USF”. `USF:<cnes>` filtra por `unidadeAtuacao === 'USF'` **e** `usfAtuacao.cnes`, preservando zeros iniciais. O enum interno `UnidadeAtuacao.USF` não foi alterado. “Todas” remove apenas o filtro local: nunca amplia a resposta autorizada. Selecionar outra instituição resulta em zero linhas, sem uma nova consulta de usuários. Cards contam a lista autorizada original; busca por nome/email, cargo, paginação e layout foram preservados. A atualização assíncrona sinaliza a detecção de mudanças do Angular.

| Área revisada | Resultado |
| --- | --- |
| Grupos Terapêuticos | Já protegidos pela unidade do coordenador, incluindo IDs diretos, sessões, participantes e contagens de grupos legados mistos. Nenhuma alteração em produção. O bloco condicional corrigido anteriormente em `GrupoTerapeuticoServiceImpl` mantém chaves e fluxo válido. |
| Agenda | Já valida unidade de profissional e paciente, consulta diária/período, IDs, status, remarcação, vagas e horários. PROFISSIONAL continua limitado à própria agenda, inclusive contra colega da mesma unidade; ADMIN/RECEP mantêm acesso aos atendimentos autorizados da unidade. Nenhuma alteração em produção. |
| Relatórios | Busca ativa usa pacientes autorizados; frequência usa grupos/coordenadores e participantes autorizados. “Todos os profissionais” permanece limitado à unidade. Profissional/grupo externo recebe 404. PDF/XLSX foram lidos nos testes para confirmar exclusões. Nenhuma alteração em serviço, template ou renderizador. |

Arquivos analisados nesta revisão (caminhos relativos ao repositório):

- `backend/buscaativa/src/main/java/com/pet/buscaativa/`: `services/impl/{UsuarioServiceImpl,EscopoUnidadeService,GrupoTerapeuticoServiceImpl,AgendamentoServiceImpl,RelatorioServiceImpl,UsuarioContextService}.java`; controllers de usuários, grupos, agenda e relatórios; repositórios, DTOs e regras de segurança utilizados nesses fluxos.
- `frontend/sentinela-pet/src/app/`: tela `pages/profissionais`, serviços de profissionais e unidades de saúde e consumidores de seleção em agenda, grupos e relatórios.
- Testes existentes de usuários, isolamento, relatórios e telas de profissionais; configuração Maven e Angular para executar as verificações sem alterar seus limites.

Arquivos efetivamente alterados nesta revisão:

- `backend/buscaativa/src/main/java/com/pet/buscaativa/services/impl/UsuarioServiceImpl.java`.
- `backend/buscaativa/src/test/java/com/pet/buscaativa/ProfissionaisEscopoTests.java` (novo).
- `backend/buscaativa/src/test/java/com/pet/buscaativa/UsuarioUnidadeTests.java`.
- `backend/buscaativa/src/test/java/com/pet/buscaativa/IsolamentoUnidadeTests.java`.
- `frontend/sentinela-pet/src/app/pages/profissionais/profissionais.ts`.
- `frontend/sentinela-pet/src/app/pages/profissionais/profissionais.html`.
- `frontend/sentinela-pet/src/app/pages/profissionais/profissionais.spec.ts`.
- Este relatório.

Cobertura acrescentada: cinco instituições (CAPS AD/I/II e duas USFs) para listagem ADMIN/RECEP, UUID externo/inexistente, PUT com payload disfarçado, DELETE, edição/remoção permitidas, `/me`, seleções e permissões de PROFISSIONAL. Os testes anteriores de edição agora autenticam um usuário da mesma unidade; a antiga expectativa de edição global de legado foi substituída por bloqueio e persistência intacta. A cobertura existente de grupos/agenda/relatórios foi complementada com CAPS II, duas USFs distintas, parâmetros externos, exportações reais e própria agenda. No frontend, os testes validam o catálogo dinâmico, CNES, CAPS, filtros combinados, cards, paginação e falha de carregamento. Nenhum teste foi desativado.

| Validação desta revisão | Resultado |
| --- | --- |
| Maven Wrapper: `bash ./mvnw -s <settings-do-ambiente> -Dmaven.repo.local=<cache-do-ambiente> -B -ntp clean test` | BUILD SUCCESS: 100 testes; zero falhas, erros ou ignorados. Wrapper executado com bash porque o arquivo existente não tem permissão de execução; modo do arquivo preservado. |
| Angular: `CI=true npm test -- --watch=false` com `--include` das telas profissionais, cadastro-profissional e detalhe-profissional | 27 testes passaram em 3 arquivos (11 da listagem, 11 do cadastro, 5 dos detalhes). |
| Angular: `npm run build` | Sucesso. Bundle inicial 845,18 kB; aviso de orçamento de 500 kB, abaixo do limite de erro existente de 1 MB. Limites preservados. |
| Revisão completa do diff e `git diff --check` | Apenas os oito arquivos relacionados acima; sem erros de espaços ou formatação. |

O teste de contexto geral ainda registra a indisponibilidade de resolução de rede da sincronização opcional SUS, tratada pelo código existente; ele passou. Os testes de escopo substituem somente essa fronteira externa por mock, exercitando JWT, controllers, serviços, H2 e exportadores reais. A suíte Angular completa não foi reexecutada nesta revisão; as falhas anteriores fora destas três telas constam no histórico abaixo.

Confirmações: administrador e recepção não recebem usuários externos; USF Centro não enxerga USF Andaiá; CAPS AD não enxerga CAPS I/II. Grupos, agenda e relatórios permanecem isolados. A regra mais restritiva da própria agenda de PROFISSIONAL foi preservada. Nenhuma funcionalidade sem relação com a tarefa foi modificada: `EscopoUnidadeService`, enums, roles, JWT, regras assistenciais, CSS, sidebar, header e layouts PDF/Excel permanecem intactos.

## Histórico da implementação anterior

O isolamento foi implementado em duas etapas: primeiro a USF específica do usuário e o formulário; depois o escopo das APIs assistenciais. A análise inicial usou o checkout atual de main, sem alterações prévias.

O modelo de Usuario recebeu ManyToOne para UsfReferencia, pela coluna usf_atuacao_id, sem cascatas de remoção ou criação. UsuarioServiceImpl resolve exclusivamente usfAtuacaoId no repositório: USF exige um ID existente; CAPS normaliza a relação para null. O DevConfig liga apenas os dois usuários USF de demonstração às USFs sintéticas já criadas.

UsuarioDTO preserva os campos anteriores e acrescenta usfAtuacaoId e usfAtuacao (id, cnes, nomeUsf). O objeto de retorno é somente leitura na desserialização; o backend ignora uma entidade/objeto de USF enviado pelo cliente. Senha é somente escrita e não aparece no JSON de resposta. UsuarioMapper mantém a proteção da senha e não cria a relação com base no objeto recebido.

O select único “Unidade de atuação” mantém CAPS AD, CAPS I e CAPS II e carrega as USFs por UnidadeSaudeService, a partir de GET /api/unidades-saude. A opção genérica USF foi removida da apresentação. As opções USF usam internamente USF:<id>, convertidas no envio em unidadeAtuacao=USF e usfAtuacaoId=<id>. CAPS envia usfAtuacaoId=null. A edição pré-seleciona a USF persistida, inclusive quando a lista chega depois dos dados do usuário, e permite USF A→B, USF→CAPS e CAPS→USF. Detalhes e listagem mantêm o layout e exibem nomeUsf quando disponível.

EscopoUnidadeService centraliza o escopo. Lê a Authentication do SecurityContextHolder; o principal Usuario é recarregado pelo filtro JWT existente a cada requisição. Há fallback pelo nome autenticado para outros principals legítimos. Nenhum parâmetro enviado pelo cliente determina a unidade autorizada. CAPS compara capsReferencia; USF compara o ID persistido de usfReferencia com usfAtuacao. Specifications e critérios Envers aplicam o mesmo escopo às consultas e revisões auditadas.

Usuário USF legado sem associação continua existindo e pode acessar sua identificação e a administração conforme os roles existentes. As consultas assistenciais falham fechadas, com a mensagem “A unidade USF específica deste usuário ainda não foi configurada.” O ADMIN pode corrigir a associação pela edição normal; essa correção foi testada pela API. USF sem relação não significa acesso a todas as USFs.

| Caminho | Proteção implementada |
| --- | --- |
| GET /api/pacientes | Lista somente pacientes autorizados. |
| GET /api/pacientes/pesquisa | Specification de unidade antes de nome/documento, risco, status, tipo, paginação e ordenação; resumo também limitado à unidade. |
| GET /api/pacientes/{idPublico} | UUID fora do escopo recebe o mesmo padrão de não encontrado usado para UUID ausente. |
| GET /api/pacientes/{idPublico}/detalhe | Validação antes dos dados e próximo atendimento. |
| GET /api/pacientes/{idPublico}/agendamentos | Paciente validado antes dos atendimentos. |
| GET /api/pacientes/busca/cpf/{cpf} | Busca no escopo; documento externo e ausente têm a mesma resposta de não encontrado. |
| GET /api/pacientes/busca/cns/{cns} | Mesma proteção da busca por CPF. |
| GET /api/pacientes/busca/nome | Busca apenas no escopo. |
| GET /api/pacientes/busca-ativa | Reclassificação existente e alertas aplicados apenas aos pacientes autorizados. |
| GET /api/pacientes/{idPublico}/historico | Paciente validado antes de buscar eventos. |
| Escritas em paciente e histórico | Validam escopo antes das alterações; duplicidade permanece validada antes de modificar a entidade gerenciada. |
| /api/inicio/resumo | Contagens, prioridades e dados derivados de atendimentos usam pacientes da unidade, mantendo os critérios anteriores. |
| /api/painel/resumo | Dados atuais e histórico Envers limitados à unidade; filtros intersectam a autorização e a lista de unidades oferece apenas a autorizada. |
| /api/agendamentos | Agenda, acesso por ID, remarcação e seleção de profissional validam unidade; criação/status também validam o paciente. |
| Configuração de agenda | UsuarioContextService acrescenta a validação de unidade aos acessos de disponibilidade, bloqueios e exceções; regras de capacidade e permissão permanecem. |
| /api/grupos | Unidade derivada do coordenador existente; valida IDs de pacientes em participação/inscrições/correções e protege grupos/sessões por ID. |
| Retornos de grupos e sessões | Ocultam participantes externos e calculam as contagens exibidas apenas com os autorizados. Operações sobre a sessão inteira rejeitam sessões legadas mistas para não alterar pacientes externos. |
| /api/relatorios/busca-ativa/pdf e /excel | Conjunto exportado limitado à unidade; período, profissional e tipo continuam filtros adicionais. Profissional externo é rejeitado. |
| /api/relatorios/frequencia-grupos/pdf e /excel | Grupo/coordenador validado; nomes e contagens de participantes externos excluídos. Layouts e renderizadores permanecem iguais. |
| /api/usuarios/profissionais-selecao e /referencia-selecao | Opções limitadas à unidade, preservando elegibilidade funcional. Administração geral de usuários mantida. |

A análise cobriu os arquivos alterados abaixo e os arquivos existentes relacionados: Paciente/UsfReferencia, UnidadeAtuacao e converter, CapsEnum e ClassificacaoRisco, DTOs de paciente/agenda/grupo/relatórios, controllers assistenciais, PacienteRepository, SessaoGrupoRepository e demais repositórios usados pelos serviços, SecurityConfig/SecurityFilter/UserDetailsServiceImpl, PoliticaClassificacaoPaciente/MonitoramentoBuscaAtiva, templates de relatórios, serviços/interfaces Angular, manifests e configuração de build/testes. Os caminhos de leitura, escrita e agregação foram conferidos antes da implementação.

Arquivos modificados, relativos a backend/buscaativa/src/main/java/com/pet/buscaativa:

- config/DevConfig.java
- controllers/UsuarioController.java
- entities/Usuario.java
- entities/dto/UsuarioDTO.java
- mapping/UsuarioMapper.java
- repositories/AgendamentoRepository.java
- repositories/UsuarioRepository.java
- services/impl/AgendamentoServiceImpl.java
- services/impl/GrupoTerapeuticoServiceImpl.java
- services/impl/HistoricoPacienteServiceImpl.java
- services/impl/InicioServiceImpl.java
- services/impl/PacienteServiceImpl.java
- services/impl/PainelHistoricoServiceImpl.java
- services/impl/PainelServiceImpl.java
- services/impl/RelatorioServiceImpl.java
- services/impl/UsuarioContextService.java
- services/impl/UsuarioServiceImpl.java

Arquivos modificados, relativos a frontend/sentinela-pet/src/app:

- pages/cadastro-profissional/cadastro-profissional.ts
- pages/cadastro-profissional/cadastro-profissional.html
- pages/cadastro-profissional/cadastro-profissional.spec.ts
- pages/detalhe-profissional/detalhe-profissional.html
- pages/detalhe-profissional/detalhe-profissional.spec.ts
- pages/profissionais/profissionais.html
- pages/profissionais/profissionais.spec.ts
- services/profissional/profissional-service.ts

Arquivos adicionados:

- backend/buscaativa/src/main/java/com/pet/buscaativa/entities/dto/UsfAtuacaoDTO.java
- backend/buscaativa/src/main/java/com/pet/buscaativa/services/impl/EscopoUnidadeService.java
- backend/buscaativa/src/test/java/com/pet/buscaativa/UsuarioUnidadeTests.java
- backend/buscaativa/src/test/java/com/pet/buscaativa/IsolamentoUnidadeTests.java
- backend/buscaativa/src/test/java/com/pet/buscaativa/PainelHistoricoEscopoTests.java
- backend/buscaativa/src/test/java/com/pet/buscaativa/MigracaoUnidadeTests.java
- backend/buscaativa/db/isolamento-unidade.sql
- docs/isolamento-por-unidade.md

Validação executada:

| Verificação | Resultado |
| --- | --- |
| Backend: mvn -B -ntp -Dlogging.level.org.springframework.security=WARN verify | 50 testes passaram; 0 falhas, 0 erros, 0 ignorados. JAR gerado. |
| UsuarioUnidadeTests | 13 testes: CAPS AD/I/II, USF válida/ausente/inexistente, normalização, trocas, DTO sem senha/objeto forjado, enum/converter, ciclo de vida independente da USF. |
| IsolamentoUnidadeTests | 34 testes com filtro JWT, MockMvc, controllers, serviços e consultas reais: quatro escopos, três roles, buscas/listas/UUID, detalhes/histórico/atendimentos, resumos, painel, agenda, inscrições/grupos mistos, PDF/XLSX, manipulação de parâmetros, USF legada e correção administrativa, paginação/ordenação/filtros e duplicidade preservada. |
| PainelHistoricoEscopoTests | Revisões Envers reais e confirmadas em transações: CAPS, duas USFs, contagens e filtros que não ampliam acesso. |
| MigracaoUnidadeTests | SQL executado no H2: linhas existentes e histórico preservados, associação inicialmente null e FK válida. |
| Frontend: npm run build | Passou. Aviso existente de orçamento: bundle inicial de 759,76 kB acima de 500 kB, abaixo do limite de erro de 1 MB. Budgets não alterados. |
| Frontend: CI=true npm test -- --watch=false | 91 testes executados: 84 passaram, 7 falharam em 6 arquivos; 5 erros assíncronos não tratados. Os 18 testes das três telas alteradas passaram. |
| git diff --check | Sem erros. Diff completo revisado. |

As 7 falhas Angular restantes estavam presentes no baseline: app.spec.ts ainda espera o título inicial do scaffold (1); DetalhePaciente usa mock sem buscarDetalhe (2); MainLayout, Inicio, NovoAgendamento e Sidebar não fornecem ActivatedRoute (4). Os erros assíncronos vêm de HTTP sem mock, incluindo /api/usuarios/me. Nenhum teste foi removido, ignorado ou teve suas asserções desativadas. Essas correções não relacionadas ficaram fora desta tarefa. A comunicação externa com o SUS é substituída apenas nos novos testes por um mock na fronteira de rede; o código de produção, as regras assistenciais, a autenticação JWT, os serviços, as consultas e os renderizadores executam de verdade.

Para um banco persistente com o schema antigo, aplique backend/buscaativa/db/isolamento-unidade.sql uma única vez antes de executar esta versão, revisando a compatibilidade com o dialeto usado. O SQL foi verificado no H2 e não foi aplicado em banco da aplicação. Acrescenta a coluna e a FK em tb_usuario e a coluna auditada em tb_usuario_aud; conserva os dados e os códigos dos enums. Cada usuário USF legado precisa receber a USF correta pela edição administrativa; não há atribuição automática por nome.

O perfil dev existente usa H2 em memória, ddl-auto=create e console H2 aberto. Ele é destinado a desenvolvimento e dados sintéticos; o isolamento implementado e testado cobre as APIs assistenciais. Um uso em produção requer configuração de banco persistente e console administrativo apropriada. Essas configurações e as regras de autenticação existentes não foram alteradas.

Confirmações explícitas:

- Não foi criada uma enum para cada USF; UnidadeAtuacao.USF permanece internamente.
- Códigos preservados: USF(1), CAPS_AD(2), CAPS_I(3), CAPS_II(4); converter intacto.
- USF específica é obrigatória em todo cadastro/edição de usuário USF; legados sem vínculo permanecem bloqueados para dados assistenciais até configuração.
- CAPS AD, CAPS I e CAPS II continuam disponíveis.
- USF genérico não aparece mais no select; nomes de USFs vêm de /api/unidades-saude.
- APIs assistenciais retornam somente pacientes autorizados pela instituição, por capsReferencia ou ID de usfReferencia.
- ADMINISTRADOR não recebe bypass global por role; PROFISSIONAL e RECEPCAO também ficam limitados à unidade.
- Filtros de tela e parâmetros nunca ampliam autorização.
- A proteção está no backend; nenhum filtro Angular substitui segurança.
- Regras não relacionadas de risco, faltas, presença, agendamento, disponibilidade, bloqueios, exceções, recorrência/frequência/inscrição retroativa, elegibilidade de referência, encerramento/reativação e auditoria foram preservadas; apenas validações e filtros de escopo foram acrescentados.
- Cadastro e campos de paciente, URLs, roles, autenticação, layout de PDF/Excel, sidebar, header, CSS e identidade visual foram preservados.

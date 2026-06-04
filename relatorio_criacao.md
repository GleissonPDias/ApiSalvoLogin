# 📊 Relatório de Criação — API Salvo

> **Projeto:** ApiSalvoLogin &nbsp;|&nbsp; **Data do Relatório:** 04/06/2026 &nbsp;|&nbsp; **Tipo:** Backend REST + WebSocket

---

## 1. Objetivo do Projeto

A **API Salvo** foi criada como backend de um aplicativo Android de **socorro automotivo sob demanda**. O sistema funciona como uma plataforma que conecta clientes que precisam de serviços mecânicos/guincho com oficinas e prestadores cadastrados na região.

### Escopo Funcional

O sistema cobre as seguintes operações:

| Módulo | Funcionalidades |
|--------|----------------|
| **Autenticação** | Login com BCrypt, cadastro com criação automática de perfil de prestador, recuperação de senha (stub) |
| **Gestão de Perfil** | Buscar perfil com avaliações agregadas, atualizar campos dinamicamente por role, upload de banner, toggle online/offline |
| **Catálogo de Serviços** | CRUD completo de serviços com preço base + preço por km |
| **Frota de Veículos (Oficina)** | CRUD com soft delete, upload de foto em Base64, campos de marca/tipo/manutenção |
| **Veículos do Cliente** | CRUD com soft delete, campos de marca/cor/tipo |
| **Sistema de Match** | Radar geográfico com `ST_Distance_Sphere`, criação de convites, notificação via WebSocket |
| **Acompanhamento (Polling)** | Status do pedido em tempo real, máquina de estados, timeout automático |
| **Avaliação** | Nota + comentário após conclusão do serviço |

---

## 2. Arquitetura Adotada

### Padrão: Routes → Repository → Database

A API segue uma arquitetura de **2 camadas simplificada**:

```
┌──────────────────────┐
│    Android App       │  ← Consome a API via Retrofit
├──────────────────────┤
│                      │
│  Routes (Controller) │  ← Recebe HTTP, valida, responde JSON
│         │            │
│  Repository (DAO)    │  ← Queries SQL diretas via JDBC
│         │            │
│  DatabaseConfig      │  ← Pool de conexões HikariCP → MySQL
│                      │
└──────────────────────┘
```

> [!NOTE]
> **Não existe camada de Service/UseCase** entre Routes e Repository. As rotas chamam diretamente as funções do repository. Isso simplifica o código mas acopla a lógica de negócio à camada de dados.

### Decisões Arquiteturais

| Decisão | Justificativa |
|---------|---------------|
| **JDBC puro** (sem ORM) | Controle total sobre queries, performance previsível, sem overhead de frameworks como Exposed ou Hibernate |
| **Singleton DatabaseConfig** | Um único pool compartilhado por toda a aplicação, gerenciado pelo HikariCP |
| **Gson** para JSON | Compatibilidade nativa com o Retrofit do Android |
| **BCrypt** para senhas | Padrão da indústria para hash de senhas com salt automático |
| **WebSocket** para notificações | Comunicação instantânea com oficinas conectadas, sem necessidade de push notifications externas |
| **Polling** para o cliente | Mais simples de implementar no Android do que WebSocket full-duplex para o cliente |
| **Soft Delete** para veículos | Preserva integridade referencial dos pedidos históricos |
| **Shadow JAR + Docker** | Deploy simplificado em um único arquivo JAR, containerizado |

---

## 3. Análise dos Módulos

### 3.1 Autenticação

| Aspecto | Análise |
|---------|---------|
| **Hash de Senha** | ✅ BCrypt com salt automático — padrão seguro |
| **Transação no Cadastro** | ✅ `autoCommit = false` + `commit/rollback` — garante atomicidade |
| **Criação automática do perfil** | ✅ Ao cadastrar um `provider`, já cria o `provider_profiles` |
| **Recuperação de Senha** | ⚠️ Apenas verifica se o e-mail existe. Falta integração com serviço de e-mail |
| **Tokens / JWT** | ❌ Não implementado. A API não autentica requisições subsequentes |

### 3.2 Sistema de Match (Radar)

Este é o módulo mais sofisticado da API:

1. **Busca Geográfica**: Usa `ST_Distance_Sphere()` do MySQL com raio de **15 km**
2. **Filtros Inteligentes**: Só encontra oficinas que estão online (`is_receiving_requests = TRUE`), oferecem o serviço solicitado (`service_type`) e têm o serviço ativo (`is_active = TRUE`)
3. **Cálculo de Preço**: `preço_final = base_price + (price_per_km × distância_km)`
4. **Estimativa de Tempo**: `minutos = max(2, distância_km × 2.5)`
5. **Convites em Batch**: Insere múltiplos `service_matches` via `addBatch()` / `executeBatch()`
6. **WebSocket Personalizado**: Cada oficina recebe um JSON com preço e distância calculados individualmente
7. **Timeout Automático**: Coroutine com `delay(3 min)` que cancela pedidos não aceitos

### 3.3 Polling e Máquina de Estados

O ciclo de vida de um pedido segue esta máquina de estados:

```
searching → accepted → en_route → arrived → in_progress → completed
     │
     └──→ canceled (timeout ou cancelamento manual)
```

O cliente usa polling (`GET /status-pedido/{id}`) para acompanhar a transição. A API retorna detalhes da oficina quando o status muda para `accepted`.

### 3.4 Gestão de Veículos

O sistema gerencia **dois tipos distintos** de veículos:
- **Veículos da Oficina** (`provider_vehicles`): Guinchos, motos de resgate — usados para atender chamados
- **Veículos do Cliente** (`customer_vehicles`): Carros pessoais que precisam de socorro

> [!NOTE]
> Ambos reutilizam a data class `ProviderVehicleResponse` como DTO, o que é um acoplamento de conveniência. Em refatoração futura, seria ideal ter DTOs separados.

---

## 4. Análise de Segurança

| Item | Status | Observação |
|------|--------|------------|
| Hash de senhas | ✅ Seguro | BCrypt com salt |
| Autenticação de rotas | ❌ Ausente | Qualquer pessoa com o endpoint pode fazer requisições |
| Credenciais do banco | ⚠️ Hardcoded | Devem ser movidas para variáveis de ambiente |
| SQL Injection | ✅ Protegido | Uso consistente de `PreparedStatement` em todas as queries |
| Validação de role | ✅ Parcial | `atualizarPerfilNoBanco` filtra campos por role |
| Rate Limiting | ❌ Ausente | Sem proteção contra abuso de requisições |
| HTTPS | ❌ Não configurado | Depende do proxy/plataforma de deploy (Render, etc.) |
| CORS | ❌ Não configurado | Não é necessário para app Android, mas seria para web |

---

## 5. Pontos de Atenção

### ⚠️ Credenciais Expostas

As credenciais do banco estão diretamente no código-fonte ([DatabaseConfig.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/DatabaseConfig.kt#L15-L17)):

```kotlin
config.jdbcUrl = "jdbc:mysql://thyagoquintas.com.br:3306/engenharia_339"
config.username = "engenharia_339"
config.password = "capivara"
```

> [!CAUTION]
> **Risco Alto**: Se o repositório for público, qualquer pessoa terá acesso total ao banco de dados. Mova para variáveis de ambiente (`System.getenv()`).

### ⚠️ Sem Autenticação JWT

Todas as rotas estão completamente abertas. Qualquer requisição HTTP é processada sem verificar a identidade do chamador.

### ⚠️ WebSocket em Memória

O mapa `prestadoresConectados` é volátil. Se o servidor reiniciar ou escalar horizontalmente, as conexões são perdidas.

### ⚠️ Upload de Banner

A rota `/atualizar-banner/{id}` salva arquivos localmente em `uploads/` e gera uma URL com placeholder (`https://sua-api.com/uploads/...`). Em produção, considere usar um serviço de armazenamento (S3, Cloudinary, etc.).

### ⚠️ Inconsistência na Main-Class

No [build.gradle.kts](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/build.gradle.kts):
- `application.mainClass` = `"com.example.MainKt"` ✅
- `ShadowJar manifest Main-Class` = `"org.example.MainKt"` ❌ (package diferente)

Isso pode causar erro ao executar o JAR diretamente.

---

## 6. Recomendações de Evolução

| Prioridade | Recomendação | Impacto |
|------------|-------------|---------|
| 🔴 Alta | Mover credenciais para variáveis de ambiente | Segurança |
| 🔴 Alta | Implementar JWT ou outro mecanismo de autenticação | Segurança |
| 🔴 Alta | Corrigir o `Main-Class` no ShadowJar manifest | Deploy |
| 🟡 Média | Criar DTOs separados para veículos do cliente | Manutenibilidade |
| 🟡 Média | Adicionar camada de Service entre Routes e Repository | Arquitetura |
| 🟡 Média | Implementar envio de e-mail na recuperação de senha | Funcionalidade |
| 🟡 Média | Usar Redis Pub/Sub para WebSocket escalável | Escalabilidade |
| 🟢 Baixa | Adicionar logging estruturado (substituir `println`) | Observabilidade |
| 🟢 Baixa | Criar testes unitários para os repositories | Qualidade |
| 🟢 Baixa | Configurar CORS e Rate Limiting | Segurança |

---

## 7. Métricas do Código

| Métrica | Valor |
|---------|-------|
| **Total de arquivos Kotlin** | 15 (excluindo BombSearch) |
| **Total de linhas de código** | ~1.300 |
| **Rotas HTTP** | 22 endpoints |
| **Rotas WebSocket** | 1 endpoint |
| **Models (Data Classes)** | 12 classes |
| **Funções de Repository** | 20 funções |
| **Tabelas MySQL utilizadas** | 7 tabelas |
| **Dependências externas** | 8 libraries |

---

## 8. Conclusão

A API Salvo é um backend funcional e bem estruturado para um MVP de aplicativo de socorro automotivo. Os destaques técnicos são:

- **Radar geográfico** com `ST_Distance_Sphere` para busca por proximidade
- **WebSocket** para notificações em tempo real
- **Transações ACID** nas operações críticas (cadastro, match, aceite)
- **Connection pool** com HikariCP para performance
- **Soft delete** para preservação de dados históricos

As principais áreas de melhoria estão na **segurança** (autenticação JWT, credenciais em env vars) e na **escalabilidade** (WebSocket com Redis, logging estruturado).

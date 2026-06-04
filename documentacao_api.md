# 📖 Documentação Técnica — API Salvo

> **Versão:** 1.0 &nbsp;|&nbsp; **Data:** 04/06/2026 &nbsp;|&nbsp; **Linguagem:** Kotlin &nbsp;|&nbsp; **Framework:** Ktor 2.3.10

---

## 📋 Índice

1. [Visão Geral](#1-visão-geral)
2. [Stack Tecnológica](#2-stack-tecnológica)
3. [Estrutura de Pastas](#3-estrutura-de-pastas)
4. [Configuração e Infraestrutura](#4-configuração-e-infraestrutura)
5. [Models (Data Classes)](#5-models-data-classes)
6. [Repositories (Camada de Dados)](#6-repositories-camada-de-dados)
7. [Routes (Endpoints da API)](#7-routes-endpoints-da-api)
8. [WebSocket — Canal em Tempo Real](#8-websocket--canal-em-tempo-real)
9. [Tabelas do Banco de Dados (MySQL)](#9-tabelas-do-banco-de-dados-mysql)
10. [Glossário de Termos](#10-glossário-de-termos)

---

## 1. Visão Geral

A **API Salvo** é o backend de um aplicativo de **socorro automotivo** (estilo "Uber de guincho/mecânico"). Ela conecta dois tipos de usuários:

| Ator | Papel (`user_role`) | O que faz |
|------|---------------------|-----------|
| **Cliente** | `customer` | Solicita socorro mecânico/guincho, cadastra seus veículos, acompanha o status do pedido, avalia o serviço |
| **Prestador / Oficina** | `provider` | Recebe chamados em tempo real via WebSocket, aceita pedidos, gerencia sua frota de veículos e catálogo de serviços |

### Fluxo Principal Resumido

```
Cliente solicita socorro ──► API cria pedido ──► Radar geográfico busca oficinas
   próximas ──► Envia notificação via WebSocket ──► Oficina aceita ──► Cliente
   acompanha status em tempo real (polling) ──► Serviço finalizado ──► Cliente avalia
```

---

## 2. Stack Tecnológica

| Tecnologia | Versão | Função |
|------------|--------|--------|
| **Kotlin** | 1.9.22 | Linguagem principal |
| **Ktor** | 2.3.10 | Framework web (servidor HTTP + WebSocket) |
| **Netty** | — | Motor HTTP embutido |
| **Gson** | — | Serialização/deserialização JSON |
| **MySQL** | 8.0 | Banco de dados relacional |
| **HikariCP** | 5.1.0 | Connection pool (pool de conexões) |
| **BCrypt** (jBCrypt) | 0.4 | Hash de senhas |
| **Logback** | 1.4.14 | Logging |
| **Shadow JAR** | 8.1.1 | Empacotamento em JAR único para deploy |
| **Docker** | — | Containerização para produção |
| **JDK** | 17 | Runtime Java |

---

## 3. Estrutura de Pastas

```
ApiSalvoLogin/
├── Dockerfile                 # Build multi-stage (compilar + executar)
├── Procfile                   # Comando para plataformas PaaS (Heroku/Render)
├── build.gradle.kts           # Dependências, plugins e configuração do Gradle
├── settings.gradle.kts        # Nome do projeto: "Api"
├── gradle.properties          # Propriedades do Gradle
│
└── src/main/kotlin/
    ├── Main.kt                # 🚀 Ponto de entrada — Configura servidor, plugins e rotas
    │
    ├── models/                # 📦 Data Classes (DTOs de entrada e saída)
    │   ├── AuthModels.kt          # Login, Cadastro, Recuperação de senha
    │   ├── MatchModels.kt         # Pedido de socorro e resposta do radar
    │   ├── PedidoModels.kt        # Listagem de pedidos, polling e aceite
    │   ├── ProviderServiceResponse.kt  # Serviço da oficina
    │   └── ProviderVehicleResponse.kt  # Veículo da oficina + request de veículo
    │
    ├── routes/                # 🌐 Definição dos Endpoints HTTP
    │   ├── AuthRoutes.kt          # /login, /cadastro, /recuperar-senha
    │   ├── MatchRoutes.kt         # /solicitar-socorro, /aceitar-socorro
    │   ├── PedidosRoutes.kt       # /listar-pedidos, /status-pedido, etc.
    │   ├── PerfilRoutes.kt        # /obter-perfil, /atualizar-perfil, etc.
    │   ├── ServicoRoutes.kt       # CRUD de serviços da oficina
    │   ├── VeiculoRoutes.kt       # CRUD de veículos (oficina + cliente) + avaliação
    │   └── WebSocketRoutes.kt     # /radar-provider/{id} (canal WS)
    │
    └── database/              # 🗄️ Acesso direto ao MySQL (Repositories)
        ├── DatabaseConfig.kt      # Singleton com HikariCP (pool de conexões)
        ├── AuthRepository.kt      # Validação de login, cadastro, recuperação
        ├── MatchRepository.kt     # Radar geográfico e criação de matches
        ├── PedidosRepository.kt   # Buscar pedidos, polling, aceitar, atualizar status
        ├── PerfilRepository.kt    # Buscar/atualizar perfil e status online
        ├── ServicoRepository.kt   # CRUD de provider_services
        └── VeiculoRepository.kt   # CRUD de veículos (oficina + cliente) + avaliação
```

---

## 4. Configuração e Infraestrutura

### 4.1 Ponto de Entrada — [Main.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/Main.kt)

O servidor Ktor é iniciado com `embeddedServer(Netty)`. A porta é definida pela variável de ambiente `PORT` (para plataformas como Render/Heroku), com fallback para `8080`.

**Plugins instalados:**
- `WebSockets` — Habilita comunicação em tempo real
- `ContentNegotiation` com `Gson` — Serializa/deserializa JSON automaticamente

**Rotas registradas:**
```kotlin
routing {
    authRoutes()       // Autenticação
    matchRoutes()      // Sistema de Match/Socorro
    pedidoRoutes()     // Gestão de Pedidos
    perfilRoutes()     // Perfis de usuário
    veiculoRoutes()    // Veículos (oficina + cliente)
    servicoRoutes()    // Serviços da oficina
    radarWebSocketRoute()  // Canal WebSocket
}
```

### 4.2 Banco de Dados — [DatabaseConfig.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/DatabaseConfig.kt)

Objeto singleton que gerencia um pool de conexões MySQL com **HikariCP**.

| Parâmetro | Valor | Descrição |
|-----------|-------|-----------|
| `maximumPoolSize` | 10 | Máximo de conexões simultâneas |
| `minimumIdle` | 2 | Conexões mínimas mantidas abertas |
| `idleTimeout` | 30s | Tempo para fechar conexões ociosas |
| `connectionTimeout` | 10s | Tempo máximo esperando uma conexão |
| `maxLifetime` | 30min | Vida máxima de uma conexão |

> [!IMPORTANT]
> As credenciais do banco estão hardcoded no código. Em produção, devem ser movidas para variáveis de ambiente.

### 4.3 Docker — [Dockerfile](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/Dockerfile)

Build multi-stage:
1. **Stage build**: Usa `eclipse-temurin:17-jdk-jammy` para compilar com `./gradlew shadowJar`
2. **Stage runtime**: Usa `eclipse-temurin:17-jre-jammy` para executar `app.jar`

### 4.4 Procfile — [Procfile](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/Procfile)

```
web: ./gradlew run
```

---

## 5. Models (Data Classes)

As models são os **DTOs** (Data Transfer Objects) que representam os dados trafegados entre o aplicativo Android e a API.

---

### 5.1 AuthModels — [AuthModels.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/models/AuthModels.kt)

#### `RegisterRequest` — Dados enviados pelo app para cadastro

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `nome` | `String` | ✅ | Nome completo do usuário |
| `email` | `String` | ✅ | E-mail (usado como login) |
| `cpf` | `String` | ✅ | CPF ou CNPJ |
| `password` | `String` | ✅ | Senha em texto plano (será hasheada com BCrypt) |
| `telefone` | `String` | ❌ | Padrão: `"Não informado"` |
| `role` | `String` | ✅ | Tipo: `"cliente"`, `"customer"`, `"prestador"`, `"provider"` ou `"oficina"` |
| `latitude` | `Double?` | ❌ | Localização GPS (padrão: `0.0`) |
| `longitude` | `Double?` | ❌ | Localização GPS (padrão: `0.0`) |

#### `LoginRequest` — Dados enviados para login

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `email` | `String` | E-mail cadastrado |
| `password` | `String` | Senha em texto plano |

#### `AuthResponse` — Resposta da API após login/cadastro

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `sucesso` | `Boolean` | `true` se a operação foi bem-sucedida |
| `message` | `String` | Mensagem descritiva |
| `userId` | `Int?` | ID do usuário no banco |
| `nome` | `String?` | Nome do usuário |
| `service` | `String?` | Tipo de serviço (se aplicável) |
| `status` | `String?` | Status do usuário |
| `hora_data` | `String?` | Data/hora da operação |
| `preco` | `Double` | Preço (padrão: `0.0`) |
| `prestador` | `String?` | Nome do prestador |
| `role` | `String` | Role do usuário (`"customer"` ou `"provider"`) |

#### `ResetPasswordRequest` — Solicitação de recuperação de senha

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `email` | `String` | E-mail para recuperação |

#### `GenericResponse` — Resposta genérica reutilizável

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `sucesso` | `Boolean` | Indicador de sucesso |
| `mensagem` | `String` | Mensagem descritiva |

---

### 5.2 MatchModels — [MatchModels.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/models/MatchModels.kt)

#### `PedidoSocorroRequest` — Cliente solicita socorro

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `customerId` | `Int` | ✅ | ID do cliente |
| `clienteNome` | `String?` | ❌ | Nome do cliente (padrão: `"Cliente"`) |
| `latitude` | `Double` | ✅ | Latitude do veículo com problema |
| `longitude` | `Double` | ✅ | Longitude do veículo com problema |
| `serviceType` | `String` | ✅ | Tipo de serviço solicitado (ex: `"Guincho"`, `"Mecânico"`) |
| `vehicleId` | `Int` | ✅ | ID do veículo do cliente que precisa de socorro |
| `description` | `String` | ✅ | Descrição do problema (ex: `"Pneu furado"`) |

#### `ProviderMatchDetail` — Detalhe de cada oficina encontrada pelo radar

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `providerId` | `Int` | ID da oficina/prestador |
| `preco` | `Double` | Preço calculado (`base_price + price_per_km × distância`) |
| `distanciaKm` | `Double` | Distância em km entre cliente e oficina |
| `minutosEstimados` | `Int` | Tempo estimado de chegada (mínimo 2 min) |

#### `PedidoSocorroResponse` — Resposta após solicitar socorro

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `sucesso` | `Boolean` | Se o pedido foi criado |
| `mensagem` | `String` | Mensagem descritiva |
| `requestId` | `Int?` | ID do pedido criado |
| `mecanicosNotificados` | `Int` | Quantas oficinas foram notificadas |
| `prestadoresMatch` | `List<ProviderMatchDetail>?` | Lista de oficinas compatíveis |

---

### 5.3 PedidoModels — [PedidoModels.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/models/PedidoModels.kt)

#### `PedidosResponse` — Pedido completo para listagem (histórico)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | `Int` | ID do pedido |
| `customer_id` | `Int` | ID do cliente |
| `service_type` | `String` | Tipo de serviço |
| `description` | `String` | Descrição do problema |
| `vehicle_info` | `String?` | Veículo formatado (ex: `"Fiat Uno - ABC1234"`) |
| `status` | `String` | Status atual do pedido |
| `assigned_provider_id` | `Int?` | ID da oficina que aceitou |
| `prestador_nome` | `String?` | Nome da oficina |
| `cliente_nome` | `String?` | Nome do cliente |
| `final_price` | `Double?` | Preço final |
| `final_distance` | `Double?` | Distância final em km |
| `destino_address` | `String?` | Endereço de destino |
| `created_at` | `String` | Data/hora de criação |
| `prestador_foto` | `String?` | URL da foto/banner do prestador |
| `veiculo_prestador_nome` | `String?` | Nome do veículo do prestador (guincho) |
| `veiculo_prestador_placa` | `String?` | Placa do veículo do prestador |
| `latitude` | `Double?` | Latitude da ocorrência |
| `longitude` | `Double?` | Longitude da ocorrência |

#### `PedidoPendenteResponse` — Pedido pendente visto pela oficina

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `matchId` | `Int` | ID do convite (`service_matches`) |
| `requestId` | `Int` | ID do pedido |
| `serviceType` | `String` | Tipo de serviço |
| `description` | `String` | Descrição do problema |
| `latitude` | `Double` | Localização do cliente |
| `longitude` | `Double` | Localização do cliente |
| `clienteNome` | `String` | Nome do cliente |
| `clienteTelefone` | `String` | Telefone do cliente |
| `veiculoMarca` | `String` | Marca do veículo do cliente |
| `veiculoModelo` | `String` | Modelo do veículo do cliente |
| `veiculoPlaca` | `String` | Placa do veículo do cliente |
| `veiculoAno` | `String` | Ano do veículo do cliente |

#### `PollingStatusResponse` — Resposta do polling (acompanhamento)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `status` | `String` | `"searching"`, `"accepted"`, `"canceled"` ou outro |
| `razaoCancelamento` | `String?` | Motivo do cancelamento (se houver) |
| `detalhesOficina` | `OficinaDetalhesPolling?` | Dados da oficina (quando aceito) |

#### `OficinaDetalhesPolling` — Detalhes da oficina para o polling

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `nome` | `String` | Nome da oficina |
| `fotoPerfil` | `String?` | URL da foto de perfil |
| `valorFinal` | `Double` | Preço final do serviço |
| `distanciaKm` | `Double` | Distância até o cliente |
| `nomeVeiculo` | `String?` | Nome do veículo que vai atender |
| `placaVeiculo` | `String?` | Placa do veículo |

#### `AceitarPedidoRequest` — Oficina aceita um pedido

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `requestId` | `Int` | ID do pedido |
| `providerId` | `Int` | ID da oficina |
| `price` | `Double` | Preço final |
| `distance` | `Double` | Distância em km |
| `vehicleId` | `Int` | ID do veículo da oficina que será usado |

---

### 5.4 ProviderServiceResponse — [ProviderServiceResponse.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/models/ProviderServiceResponse.kt)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | `Int` | ID do serviço |
| `provider_id` | `Int` | ID da oficina dona do serviço |
| `service_type` | `String` | Tipo (ex: `"Guincho"`, `"Troca de Pneu"`) |
| `base_price` | `Double` | Preço base fixo |
| `price_per_km` | `Double` | Adicional por km rodado |
| `is_active` | `Boolean` | Se está ativo e recebendo pedidos |

---

### 5.5 ProviderVehicleResponse & VeiculoRequest — [ProviderVehicleResponse.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/models/ProviderVehicleResponse.kt)

#### `ProviderVehicleResponse` — Resposta com dados de veículo da oficina

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | `Int` | ID do veículo |
| `provider_id` | `Int` | ID da oficina dona |
| `name` | `String` | Nome (ex: `"Guincho Mercedes"`) |
| `plate` | `String` | Placa do veículo |
| `status` | `String` | Status (ex: `"Disponível"`, `"Em atendimento"`) |
| `vehicle_photo` | `String?` | Foto em Base64 ou URL |
| `is_active` | `Boolean` | Se está ativo |
| `brand` | `String?` | Marca |
| `vehicle_type` | `String?` | Tipo (ex: `"Guincho"`, `"Moto"`) |
| `maintenance_date` | `String?` | Data da próxima manutenção |

#### `VeiculoRequest` — Dados enviados para criar/atualizar veículo da oficina

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `id` | `Int?` | ❌ | ID (preenchido na atualização) |
| `provider_id` | `Int?` | ✅ | ID da oficina |
| `name` | `String` | ✅ | Nome do veículo |
| `plate` | `String` | ✅ | Placa |
| `status` | `String?` | ❌ | Padrão: `"Disponível"` |
| `brand` | `String?` | ❌ | Marca |
| `vehicle_type` | `String?` | ❌ | Tipo do veículo |
| `maintenance_date` | `String?` | ❌ | Data de manutenção |
| `vehicle_photo` | `String?` | ❌ | Imagem em Base64 |

---

## 6. Repositories (Camada de Dados)

Cada repository contém funções que fazem queries SQL diretas no MySQL via JDBC/HikariCP.

---

### 6.1 AuthRepository — [AuthRepository.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/AuthRepository.kt)

| Função | Descrição |
|--------|-----------|
| `validarNoBanco(email, senhaDigitada)` | Busca o usuário por e-mail, compara a senha com BCrypt. Retorna `AuthResponse` com `userId`, `nome` e `role` |
| `cadastrarNoBanco(usuario)` | Insere na tabela `users` com senha hasheada. Se `role = provider`, cria automaticamente um registro em `provider_profiles`. Usa **transação** |
| `solicitarRecuperacao(email)` | Verifica se o e-mail existe. Retorna `GenericResponse` (futuro: enviar e-mail real) |

---

### 6.2 MatchRepository — [MatchRepository.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/MatchRepository.kt)

| Função | Descrição |
|--------|-----------|
| `solicitarSocorroRadar(pedido)` | **O coração da API.** 1) Cria pedido em `service_requests` com status `searching`. 2) Executa o **Radar Geográfico** usando `ST_Distance_Sphere()` do MySQL para encontrar oficinas em um raio de 15 km que estejam online, ofereçam o serviço solicitado e tenham o serviço ativo. 3) Calcula preço final: `base_price + (price_per_km × distância)`. 4) Cria convites em `service_matches` com status `pending`. Usa **transação** |

---

### 6.3 PedidosRepository — [PedidosRepository.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/PedidosRepository.kt)

| Função | Descrição |
|--------|-----------|
| `buscarPedidos(userId)` | Lista todos os pedidos do **cliente** com JOINs para trazer dados do prestador, veículo do prestador e veículo do cliente. Ordenado por data DESC |
| `buscarPedidosDoPrestador(providerId)` | Lista convites **pendentes** (`status = 'pending'`) para a oficina, com dados do cliente e do veículo |
| `buscarHistoricoDaOficina(providerId)` | Lista histórico completo de pedidos que a oficina já aceitou (todos os status exceto `searching`) |
| `verificarStatusDoPedidoBanco(requestId)` | **Polling do cliente.** Retorna o status atual do pedido. Se `accepted`, busca detalhes da oficina (nome, veículo, preço). Se `canceled`, retorna o motivo |
| `aceitarPedidoBanco(dados)` | Atualiza `service_requests` para `accepted` com preço e distância finais. Atualiza o `service_matches` correspondente. Usa **transação** com verificação de concorrência (`WHERE status = 'searching'`) |
| `atualizarStatusPedidoBanco(pedidoId, providerId, status)` | Atualiza o status do pedido (ex: `en_route`, `in_progress`, `completed`). Verifica que o prestador é o dono |

---

### 6.4 PerfilRepository — [PerfilRepository.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/PerfilRepository.kt)

| Função | Descrição |
|--------|-----------|
| `atualizarPerfilNoBanco(userId, campos)` | Atualiza campos do perfil dinamicamente. Filtra campos permitidos por role: **provider** pode editar nome, CPF, endereço, banner, telefone, coordenadas, fotos. **customer** pode editar nome e telefone |
| `buscarPerfilNoBanco(id)` | Busca perfil com média de avaliações (`AVG(rating)`) e total de reviews via JOIN com `service_reviews`. Retorna mapa com nome, CNPJ, endereço, banner, fotos, rating e reviews |
| `atualizarStatusOnline(providerId, isOnline)` | Alterna o campo `is_receiving_requests` na tabela `provider_profiles` (0/1) |

---

### 6.5 ServicoRepository — [ServicoRepository.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/ServicoRepository.kt)

| Função | Descrição |
|--------|-----------|
| `adicionarServicoNoBanco(...)` | Insere na `provider_services` com `is_active = 1` |
| `buscarServicosDaOficina(providerId)` | Lista todos os serviços (ativos e inativos) ordenados por ID DESC |
| `atualizarDadosServicoNoBanco(...)` | Atualiza tipo, preço base e preço por km |
| `alternarStatusServicoNoBanco(...)` | Ativa/desativa um serviço |
| `excluirServicoNoBanco(id, providerId)` | **Exclusão definitiva** (DELETE) |

---

### 6.6 VeiculoRepository — [VeiculoRepository.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/database/VeiculoRepository.kt)

**Veículos da Oficina (`provider_vehicles`):**

| Função | Descrição |
|--------|-----------|
| `adicionarVeiculoNoBanco(providerId, veiculo)` | Insere com `is_active = 1` e todos os campos (foto, marca, tipo, manutenção) |
| `buscarVeiculosDaOficina(providerId)` | Lista veículos ativos (`is_active = 1`) |
| `atualizarStatusVeiculoNoBanco(id, providerId, status)` | Atualiza o status textual (ex: `"Disponível"` → `"Em atendimento"`) |
| `excluirVeiculoNoBanco(id, providerId)` | **Soft delete** (`is_active = 0`) |
| `atualizarDadosVeiculoNoBanco(providerId, veiculo)` | Atualiza todos os campos. Se veio foto nova, atualiza; senão, mantém a existente |

**Veículos do Cliente (`customer_vehicles`):**

| Função | Descrição |
|--------|-----------|
| `adicionarVeiculoClienteNoBanco(...)` | Insere com modelo, placa, marca, cor e tipo |
| `buscarVeiculosDoCliente(customerId)` | Lista veículos ativos. Reutiliza `ProviderVehicleResponse` como DTO |
| `excluirVeiculoClienteNoBanco(id, customerId)` | **Soft delete** (`is_active = 0`) |
| `atualizarDadosVeiculoClienteNoBanco(...)` | Atualiza modelo e placa |

**Avaliação:**

| Função | Descrição |
|--------|-----------|
| `salvarAvaliacaoNoBanco(pedidoId, nota, comentario)` | Insere em `service_reviews` copiando `customer_id` e `provider_id` diretamente da `service_requests` via subquery |

---

## 7. Routes (Endpoints da API)

---

### 7.1 Autenticação — [AuthRoutes.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/routes/AuthRoutes.kt)

---

#### `POST /login`

Autentica um usuário existente.

**Corpo da requisição:**
```json
{
    "email": "joao@email.com",
    "password": "minhasenha123"
}
```

**Resposta de sucesso (200):**
```json
{
    "sucesso": true,
    "message": "Login realizado com sucesso!",
    "userId": 42,
    "nome": "João Silva",
    "role": "customer"
}
```

**Resposta de erro (401):**
```json
{
    "sucesso": false,
    "message": "E-mail ou senha incorretos"
}
```

---

#### `POST /cadastro`

Cria uma nova conta de usuário.

**Corpo da requisição:**
```json
{
    "nome": "Oficina do Zé",
    "email": "ze@oficina.com",
    "cpf": "12.345.678/0001-99",
    "password": "senha123",
    "telefone": "(11) 99999-0000",
    "role": "prestador",
    "latitude": -23.5505,
    "longitude": -46.6333
}
```

**Resposta de sucesso (201):**
```json
{
    "sucesso": true,
    "message": "Usuário cadastrado com sucesso!",
    "userId": 43,
    "nome": "Oficina do Zé",
    "role": "provider"
}
```

> [!NOTE]
> Se o `role` for `"provider"`, a API automaticamente cria um registro em `provider_profiles` com `is_receiving_requests = 0` (offline).

---

#### `POST /recuperar-senha`

Solicita recuperação de senha.

**Corpo:** `{ "email": "joao@email.com" }`

**Resposta (200):**
```json
{
    "sucesso": true,
    "mensagem": "E-mail de recuperação enviado!"
}
```

> [!WARNING]
> Atualmente, o envio de e-mail **não está implementado**. A API apenas verifica se o e-mail existe no banco.

---

### 7.2 Match / Socorro — [MatchRoutes.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/routes/MatchRoutes.kt)

---

#### `POST /solicitar-socorro`

O cliente solicita socorro. A API executa o radar geográfico, cria convites e notifica oficinas via WebSocket.

**Corpo da requisição:**
```json
{
    "customerId": 42,
    "clienteNome": "João Silva",
    "latitude": -23.5505,
    "longitude": -46.6333,
    "serviceType": "Guincho",
    "vehicleId": 7,
    "description": "Carro não liga, bateria descarregada"
}
```

**Resposta de sucesso (201):**
```json
{
    "sucesso": true,
    "mensagem": "Pedido criado! Procurando socorro...",
    "requestId": 101,
    "mecanicosNotificados": 3,
    "prestadoresMatch": [
        { "providerId": 5, "preco": 85.50, "distanciaKm": 2.3, "minutosEstimados": 6 },
        { "providerId": 12, "preco": 120.00, "distanciaKm": 5.1, "minutosEstimados": 13 }
    ]
}
```

> [!IMPORTANT]
> **Bomba de Tempo**: Após 3 minutos, se nenhuma oficina aceitar, o pedido é cancelado automaticamente com motivo `timeout_no_provider`. Isso é feito via `launch` (coroutine assíncrona) na própria rota.

> [!NOTE]
> **WebSocket**: Cada oficina encontrada que estiver conectada ao WebSocket (`/radar-provider/{id}`) recebe um JSON personalizado com preço e distância calculados especificamente para ela.

---

#### `POST /aceitar-socorro`

Uma oficina aceita um pedido de socorro.

**Corpo da requisição:**
```json
{
    "requestId": 101,
    "providerId": 5,
    "price": 85.50,
    "distance": 2.3,
    "vehicleId": 15
}
```

**Resposta de sucesso (200):**
```json
{ "sucesso": true, "mensagem": "Corrida aceita com sucesso!" }
```

**Resposta de conflito (409):**
```json
{ "sucesso": false, "mensagem": "Ops! Outro prestador já aceitou essa chamada." }
```

---

### 7.3 Pedidos — [PedidosRoutes.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/routes/PedidosRoutes.kt)

---

#### `GET /listar-pedidos?userId={id}`

Retorna o histórico de pedidos do **cliente**.

**Query Param:** `userId` (Int, obrigatório)

**Resposta (200):** Array de `PedidosResponse`

---

#### `GET /status-pedido/{id}`

**Polling do app do cliente** — Verifica se o pedido foi aceito, cancelado ou ainda está buscando.

**Path Param:** `id` = ID do pedido

**Resposta (200):**
```json
{
    "status": "accepted",
    "detalhesOficina": {
        "nome": "Oficina do Zé",
        "fotoPerfil": null,
        "valorFinal": 85.50,
        "distanciaKm": 2.3,
        "nomeVeiculo": "Guincho Mercedes",
        "placaVeiculo": "XYZ-9876"
    }
}
```

---

#### `GET /listar-pedidos-oficina?providerId={id}`

Retorna o histórico de pedidos da **oficina** (todos os status exceto `searching`).

**Query Param:** `providerId` (Int, obrigatório)

---

#### `PATCH /atualizar-status-pedido/{id}`

Atualiza o status de um pedido em andamento.

**Path Param:** `id` = ID do pedido

**Corpo:**
```json
{
    "provider_id": "5",
    "status": "en_route"
}
```

**Status possíveis:** `en_route` → `arrived` → `in_progress` → `completed`

---

### 7.4 Perfil — [PerfilRoutes.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/routes/PerfilRoutes.kt)

---

#### `GET /obter-perfil/{id}`

Busca os dados do perfil de um usuário, incluindo média de avaliações.

**Resposta (200):**
```json
{
    "nome": "Oficina do Zé",
    "cnpj": "12.345.678/0001-99",
    "endereco": "Rua das Flores, 123",
    "banner": "https://...",
    "foto_1": "base64...",
    "foto_2": "base64...",
    "rating": "4.8",
    "reviews": 25
}
```

---

#### `PATCH /atualizar-perfil/{id}`

Atualiza campos do perfil. Os campos aceitos dependem do `role` do usuário.

**Corpo (exemplo para provider):**
```json
{
    "user_name": "Nova Oficina do Zé",
    "user_address": "Rua Nova, 456",
    "latitude": "-23.55",
    "longitude": "-46.63"
}
```

---

#### `GET /servicos-publicos/{id}`

Retorna apenas os serviços **ativos** de uma oficina (para exibição no perfil público).

---

#### `POST /atualizar-banner/{id}`

Upload de banner via **multipart/form-data**. Salva o arquivo na pasta `uploads/`.

> [!WARNING]
> A URL gerada (`https://sua-api.com/uploads/...`) está com placeholder. Deve ser configurada com o domínio real em produção.

---

#### `POST /provider/toggle-status`

Alterna o status online/offline do prestador.

**Corpo:**
```json
{
    "provider_id": "5",
    "is_online": "true"
}
```

---

### 7.5 Serviços da Oficina — [ServicoRoutes.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/routes/ServicoRoutes.kt)

CRUD completo do catálogo de serviços oferecidos por uma oficina.

---

#### `GET /servicos-oficina/{providerId}`

Lista todos os serviços (ativos e inativos) da oficina.

**Resposta (200):** Array de `ProviderServiceResponse`

---

#### `POST /adicionar-servico`

Cadastra um novo serviço.

**Corpo:**
```json
{
    "provider_id": "5",
    "service_type": "Troca de Pneu",
    "base_price": "50.00",
    "price_per_km": "3.50"
}
```

---

#### `PUT /atualizar-servico/{id}`

Atualiza tipo e preços de um serviço.

**Corpo:** Mesmo formato do POST, incluindo `provider_id`

---

#### `PATCH /alternar-status-servico/{id}`

Ativa ou desativa um serviço via switch.

**Corpo:**
```json
{
    "provider_id": "5",
    "is_active": "true"
}
```

---

#### `DELETE /excluir-servico/{id}/{providerId}`

Remove permanentemente o serviço do catálogo.

---

### 7.6 Veículos — [VeiculoRoutes.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/routes/VeiculoRoutes.kt)

#### Veículos da Oficina (`provider_vehicles`)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/veiculos-oficina/{providerId}` | Lista veículos ativos |
| `POST` | `/adicionar-veiculo` | Cadastra novo veículo (JSON `VeiculoRequest`) |
| `PATCH` | `/atualizar-status-veiculo/{id}` | Atualiza status textual |
| `PUT` | `/atualizar-veiculo/{id}` | Atualiza dados completos |
| `DELETE` | `/excluir-veiculo/{id}/{providerId}` | Soft delete |

#### Veículos do Cliente (`customer_vehicles`)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/veiculos-cliente/{customerId}` | Lista veículos ativos |
| `POST` | `/adicionar-veiculo-cliente` | Cadastra veículo (Map) |
| `PUT` | `/atualizar-veiculo-cliente/{id}` | Atualiza dados |
| `DELETE` | `/excluir-veiculo-cliente/{id}/{customerId}` | Soft delete |

#### Avaliação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/avaliar-pedido` | Salva nota e comentário para um pedido finalizado |

**Corpo do `/avaliar-pedido`:**
```json
{
    "pedidoId": "101",
    "nota": "5",
    "comentario": "Excelente atendimento!"
}
```

---

## 8. WebSocket — Canal em Tempo Real

### [WebSocketRoutes.kt](file:///c:/Users/GleissonBdf/Documents/GitHub/ApiSalvoLogin/src/main/kotlin/routes/WebSocketRoutes.kt)

#### `WS /radar-provider/{id}`

Canal WebSocket que mantém a oficina conectada para receber chamados em tempo real.

**Funcionamento:**
1. A oficina conecta via WebSocket com seu `providerId` na URL
2. A sessão é armazenada em um `ConcurrentHashMap<Int, DefaultWebSocketServerSession>` em memória
3. Quando um cliente solicita socorro (`POST /solicitar-socorro`), a API envia um `Frame.Text` com JSON customizado para cada oficina encontrada pelo radar
4. Se a oficina desconectar, sua sessão é removida do mapa

**JSON enviado via WebSocket (exemplo):**
```json
{
    "requestId": 101,
    "rawPreco": 85.50,
    "rawDistancia": 2.3,
    "veiculo": "Solicitação de Guincho",
    "defeito": "🔧 Carro não liga",
    "preco": "R$ 85,50",
    "distanciaText": "Distância: 2.3 km  •  ~6 min",
    "clienteNome": "João Silva",
    "clienteNota": "⭐ 4.9 (Nova solicitação)"
}
```

> [!IMPORTANT]
> O mapa `prestadoresConectados` vive em **memória do servidor**. Se o servidor reiniciar, todas as conexões WebSocket são perdidas. Em produção, considere soluções como Redis Pub/Sub.

---

## 9. Tabelas do Banco de Dados (MySQL)

> As tabelas foram inferidas a partir das queries SQL presentes no código. Campos marcados com `*` são obrigatórios.

### `users`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `user_id` * | INT (PK, AUTO_INCREMENT) | Identificador único |
| `user_name` * | VARCHAR | Nome completo |
| `user_email` * | VARCHAR (UNIQUE) | E-mail (login) |
| `user_password` * | VARCHAR | Hash BCrypt da senha |
| `user_cpf_cnpj` * | VARCHAR | CPF ou CNPJ |
| `user_phone` | VARCHAR | Telefone |
| `user_role` * | VARCHAR | `"customer"` ou `"provider"` |
| `user_address` | VARCHAR | Endereço completo |
| `user_banner` | TEXT | URL ou Base64 da foto de banner |
| `foto_1` | TEXT | Foto adicional 1 |
| `foto_2` | TEXT | Foto adicional 2 |
| `latitude` | DOUBLE | Latitude GPS |
| `longitude` | DOUBLE | Longitude GPS |
| `updated_at` | TIMESTAMP | Última atualização |

### `provider_profiles`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `provider_id` * | INT (PK, FK → users) | ID do prestador |
| `is_receiving_requests` * | TINYINT(1) | Se está online (0=off, 1=on) |

### `provider_services`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `id` * | INT (PK, AUTO_INCREMENT) | Identificador |
| `provider_id` * | INT (FK → users) | ID da oficina |
| `service_type` * | VARCHAR | Tipo do serviço |
| `base_price` * | DOUBLE | Preço base fixo |
| `price_per_km` * | DOUBLE | Preço por km adicional |
| `is_active` * | TINYINT(1) | Ativo (1) ou inativo (0) |

### `provider_vehicles`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `id` * | INT (PK, AUTO_INCREMENT) | Identificador |
| `provider_id` * | INT (FK → users) | ID da oficina |
| `name` * | VARCHAR | Nome do veículo |
| `plate` * | VARCHAR | Placa |
| `status` | VARCHAR | Status textual |
| `vehicle_photo` | TEXT | Foto (Base64 ou URL) |
| `brand` | VARCHAR | Marca |
| `vehicle_type` | VARCHAR | Tipo (Guincho, Moto, etc.) |
| `maintenance_date` | DATE | Data de manutenção |
| `is_active` * | TINYINT(1) | Ativo (1) ou soft-deleted (0) |
| `updated_at` | TIMESTAMP | Última atualização |

### `customer_vehicles`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `id` * | INT (PK, AUTO_INCREMENT) | Identificador |
| `customer_id` * | INT (FK → users) | ID do cliente |
| `brand` | VARCHAR | Marca |
| `model` * | VARCHAR | Modelo |
| `plate` * | VARCHAR | Placa |
| `color` | VARCHAR | Cor |
| `year` | VARCHAR | Ano |
| `vehicle_type` | VARCHAR | Tipo do veículo |
| `is_active` * | TINYINT(1) | Ativo (1) ou soft-deleted (0) |
| `updated_at` | TIMESTAMP | Última atualização |

### `service_requests`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `id` * | INT (PK, AUTO_INCREMENT) | Identificador do pedido |
| `customer_id` * | INT (FK → users) | ID do cliente |
| `vehicle_id` | INT (FK → customer_vehicles) | Veículo do cliente |
| `service_type` * | VARCHAR | Tipo de serviço solicitado |
| `description` * | TEXT | Descrição do problema |
| `location_lat` | DOUBLE | Latitude da ocorrência |
| `location_lng` | DOUBLE | Longitude da ocorrência |
| `status` * | VARCHAR | `searching`, `accepted`, `en_route`, `arrived`, `in_progress`, `completed`, `canceled` |
| `assigned_provider_id` | INT (FK → users) | Oficina que aceitou |
| `provider_vehicle_id` | INT (FK → provider_vehicles) | Veículo do prestador usado |
| `final_price` | DOUBLE | Preço final combinado |
| `final_distance` | DOUBLE | Distância final em km |
| `destino_address` | VARCHAR | Endereço de destino |
| `cancellation_reason` | VARCHAR | Motivo do cancelamento |
| `created_at` * | TIMESTAMP | Data/hora de criação |
| `updated_at` | TIMESTAMP | Última atualização |

### `service_matches`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `id` * | INT (PK, AUTO_INCREMENT) | Identificador |
| `request_id` * | INT (FK → service_requests) | Pedido relacionado |
| `provider_id` * | INT (FK → users) | Oficina convidada |
| `status` * | VARCHAR | `pending`, `accepted` |

### `service_reviews`
| Coluna | Tipo (inferido) | Descrição |
|--------|-----------------|-----------|
| `id` * | INT (PK, AUTO_INCREMENT) | Identificador |
| `request_id` * | INT (FK → service_requests) | Pedido avaliado |
| `customer_id` * | INT (FK → users) | Cliente que avaliou |
| `provider_id` * | INT (FK → users) | Prestador avaliado |
| `rating` * | INT | Nota (1–5) |
| `comment` | TEXT | Comentário opcional |

---

## 10. Glossário de Termos

| Termo | Significado |
|-------|-------------|
| **Provider / Prestador / Oficina** | Empresa ou profissional que presta o serviço automotivo |
| **Customer / Cliente** | Usuário final que solicita o socorro |
| **Match** | Convite enviado para uma oficina. Uma solicitação pode gerar múltiplos matches |
| **Radar** | Busca geográfica usando `ST_Distance_Sphere()` para encontrar oficinas dentro do raio de 15 km |
| **Polling** | O app do cliente consulta repetidamente o endpoint `/status-pedido/{id}` para saber se uma oficina aceitou |
| **Soft Delete** | Exclusão lógica — o registro não é removido do banco, apenas marcado com `is_active = 0` |
| **Bomba de Tempo** | Timer de 3 minutos que cancela automaticamente pedidos não aceitos |
| **HikariCP** | Library de connection pool para manter conexões MySQL abertas e reutilizá-las |
| **Shadow JAR** | Plugin Gradle que empacota tudo (código + dependências) em um único arquivo `.jar` |
| **Base Price** | Preço base fixo de um serviço, independente da distância |
| **Price Per Km** | Valor adicional cobrado por cada quilômetro de distância entre cliente e oficina |

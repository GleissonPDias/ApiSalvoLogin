# 📐 Fluxogramas — API Salvo

> Diagramas visuais completos do projeto para entendimento do fluxo de dados, arquitetura e relacionamentos.

---

## 1. Arquitetura Geral do Sistema

```mermaid
graph TB
    subgraph "📱 Aplicativo Android"
        A1["App Cliente"]
        A2["App Prestador"]
    end

    subgraph "🌐 API Ktor (Servidor)"
        R["Routes<br/>(Endpoints HTTP + WebSocket)"]
        REPO["Repositories<br/>(Queries SQL)"]
        DC["DatabaseConfig<br/>(HikariCP Pool)"]
        WS["WebSocket Manager<br/>(ConcurrentHashMap)"]
    end

    subgraph "🗄️ Banco de Dados MySQL"
        DB["MySQL 8.0<br/>7 tabelas"]
    end

    A1 -- "HTTP REST (JSON)" --> R
    A2 -- "HTTP REST (JSON)" --> R
    A2 -- "WebSocket" --> WS
    R --> REPO
    WS --> R
    REPO --> DC
    DC --> DB

    style A1 fill:#4CAF50,color:#fff
    style A2 fill:#FF9800,color:#fff
    style R fill:#2196F3,color:#fff
    style REPO fill:#9C27B0,color:#fff
    style DC fill:#607D8B,color:#fff
    style WS fill:#E91E63,color:#fff
    style DB fill:#795548,color:#fff
```

---

## 2. Diagrama de Entidade-Relacionamento (ER)

```mermaid
erDiagram
    users ||--o{ service_requests : "cria (customer)"
    users ||--o{ service_requests : "aceita (provider)"
    users ||--o| provider_profiles : "tem perfil"
    users ||--o{ provider_services : "oferece"
    users ||--o{ provider_vehicles : "possui"
    users ||--o{ customer_vehicles : "possui"
    users ||--o{ service_reviews : "avalia (customer)"
    users ||--o{ service_reviews : "recebe (provider)"

    service_requests ||--o{ service_matches : "gera convites"
    service_requests ||--o| service_reviews : "recebe avaliacao"
    customer_vehicles ||--o{ service_requests : "vinculado ao pedido"
    provider_vehicles ||--o{ service_requests : "usado no atendimento"

    users {
        int user_id PK
        string user_name
        string user_email
        string user_password
        string user_cpf_cnpj
        string user_phone
        string user_role
        string user_address
        string user_banner
        double latitude
        double longitude
    }

    provider_profiles {
        int provider_id PK_FK
        boolean is_receiving_requests
    }

    provider_services {
        int id PK
        int provider_id FK
        string service_type
        double base_price
        double price_per_km
        boolean is_active
    }

    provider_vehicles {
        int id PK
        int provider_id FK
        string name
        string plate
        string status
        string vehicle_photo
        string brand
        string vehicle_type
        boolean is_active
    }

    customer_vehicles {
        int id PK
        int customer_id FK
        string brand
        string model
        string plate
        string color
        string year
        boolean is_active
    }

    service_requests {
        int id PK
        int customer_id FK
        int vehicle_id FK
        string service_type
        string description
        double location_lat
        double location_lng
        string status
        int assigned_provider_id FK
        int provider_vehicle_id FK
        double final_price
        double final_distance
        string cancellation_reason
    }

    service_matches {
        int id PK
        int request_id FK
        int provider_id FK
        string status
    }

    service_reviews {
        int id PK
        int request_id FK
        int customer_id FK
        int provider_id FK
        int rating
        string comment
    }
```

---

## 3. Fluxo Completo — Solicitação de Socorro

Este é o fluxo principal da aplicação, de ponta a ponta:

```mermaid
flowchart TD
    A["🙋 Cliente abre o app"] --> B["Seleciona veículo e tipo de serviço"]
    B --> C["Descreve o problema"]
    C --> D["📡 POST /solicitar-socorro"]
    
    D --> E{"API: Criar pedido no banco<br/>(status: searching)"}
    E --> F["🛰️ Radar Geográfico<br/>ST_Distance_Sphere() - Raio 15km"]
    
    F --> G{"Encontrou oficinas?"}
    
    G -- "❌ Nenhuma" --> H["Responde: 0 mecânicos notificados"]
    H --> I["⏱️ Timer 3 min inicia"]
    I --> J{"Timer expirou?"}
    J -- "Sim" --> K["❌ Cancela pedido<br/>(timeout_no_provider)"]
    
    G -- "✅ Sim" --> L["Calcula preço individual:<br/>base_price + price_per_km × distância"]
    L --> M["Cria convites na tabela<br/>service_matches (pending)"]
    M --> N["⏱️ Timer 3 min inicia"]
    
    M --> O{"Oficina conectada<br/>no WebSocket?"}
    O -- "✅ Sim" --> P["📲 Envia JSON personalizado<br/>via WebSocket Frame.Text"]
    O -- "❌ Não" --> Q["Convite fica pendente<br/>para busca via polling"]
    
    P --> R["👨‍🔧 Oficina vê o chamado"]
    R --> S{"Oficina aceita?"}
    
    S -- "✅ Aceita" --> T["POST /aceitar-socorro"]
    T --> U["Atualiza service_requests:<br/>status = accepted<br/>assigned_provider_id<br/>final_price, final_distance"]
    U --> V["Atualiza service_matches:<br/>status = accepted"]
    
    S -- "❌ Ignora" --> N
    N --> W{"Timer 3 min expirou<br/>e ninguém aceitou?"}
    W -- "Sim" --> K
    
    V --> X["🔄 Cliente faz polling<br/>GET /status-pedido/{id}"]
    X --> Y["API retorna: accepted +<br/>detalhes da oficina"]
    
    Y --> Z["🚗 Oficina a caminho<br/>(en_route)"]
    Z --> AA["📍 Oficina chegou<br/>(arrived)"]
    AA --> AB["🔧 Serviço em andamento<br/>(in_progress)"]
    AB --> AC["✅ Serviço concluído<br/>(completed)"]
    AC --> AD["⭐ Cliente avalia<br/>POST /avaliar-pedido"]

    style A fill:#4CAF50,color:#fff
    style D fill:#2196F3,color:#fff
    style F fill:#FF5722,color:#fff
    style K fill:#f44336,color:#fff
    style P fill:#E91E63,color:#fff
    style T fill:#FF9800,color:#fff
    style AD fill:#FFD700,color:#000
```

---

## 4. Máquina de Estados — Status do Pedido

```mermaid
stateDiagram-v2
    [*] --> searching : POST /solicitar-socorro

    searching --> accepted : Oficina aceita<br/>POST /aceitar-socorro
    searching --> canceled : Timeout 3 min<br/>ou cancelamento

    accepted --> en_route : Oficina saiu<br/>PATCH /atualizar-status-pedido
    
    en_route --> arrived : Oficina chegou<br/>PATCH /atualizar-status-pedido
    
    arrived --> in_progress : Serviço iniciado<br/>PATCH /atualizar-status-pedido
    
    in_progress --> completed : Serviço finalizado<br/>PATCH /atualizar-status-pedido

    accepted --> canceled : Cancelamento
    en_route --> canceled : Cancelamento
    
    completed --> [*]
    canceled --> [*]

    note right of searching : Cliente faz polling em<br/>GET /status-pedido/{id}
    note right of canceled : cancellation_reason:<br/>timeout_no_provider<br/>ou motivo manual
```

---

## 5. Fluxo de Autenticação

```mermaid
flowchart TD
    subgraph "📝 Cadastro"
        C1["App envia dados<br/>POST /cadastro"] --> C2["API hasheia senha<br/>com BCrypt"]
        C2 --> C3["Insere na tabela users"]
        C3 --> C4{"Role = provider?"}
        C4 -- "Sim" --> C5["Cria registro em<br/>provider_profiles<br/>(offline por padrão)"]
        C4 -- "Não" --> C6["Retorna userId + nome + role"]
        C5 --> C6
    end

    subgraph "🔑 Login"
        L1["App envia email + senha<br/>POST /login"] --> L2["Busca user por email"]
        L2 --> L3{"Encontrou?"}
        L3 -- "Não" --> L4["❌ Email ou senha incorretos"]
        L3 -- "Sim" --> L5["BCrypt.checkpw()"]
        L5 --> L6{"Senha confere?"}
        L6 -- "Não" --> L4
        L6 -- "Sim" --> L7["✅ Retorna userId + nome + role"]
    end

    subgraph "🔒 Recuperar Senha"
        R1["POST /recuperar-senha"] --> R2{"Email existe<br/>no banco?"}
        R2 -- "Sim" --> R3["✅ Email de recuperação enviado<br/>(stub - não envia de fato)"]
        R2 -- "Não" --> R4["❌ Email não encontrado"]
    end

    style C5 fill:#FF9800,color:#fff
    style L7 fill:#4CAF50,color:#fff
    style L4 fill:#f44336,color:#fff
```

---

## 6. Mapa Completo de Rotas

```mermaid
graph LR
    subgraph "🔐 Autenticação"
        A1["POST /login"]
        A2["POST /cadastro"]
        A3["POST /recuperar-senha"]
    end

    subgraph "🎯 Match / Socorro"
        M1["POST /solicitar-socorro"]
        M2["POST /aceitar-socorro"]
    end

    subgraph "📋 Pedidos"
        P1["GET /listar-pedidos"]
        P2["GET /status-pedido/{id}"]
        P3["GET /listar-pedidos-oficina"]
        P4["PATCH /atualizar-status-pedido/{id}"]
    end

    subgraph "👤 Perfil"
        PF1["GET /obter-perfil/{id}"]
        PF2["PATCH /atualizar-perfil/{id}"]
        PF3["GET /servicos-publicos/{id}"]
        PF4["POST /atualizar-banner/{id}"]
        PF5["POST /provider/toggle-status"]
    end

    subgraph "🔧 Serviços"
        S1["GET /servicos-oficina/{providerId}"]
        S2["POST /adicionar-servico"]
        S3["PUT /atualizar-servico/{id}"]
        S4["PATCH /alternar-status-servico/{id}"]
        S5["DELETE /excluir-servico/{id}/{providerId}"]
    end

    subgraph "🚗 Veículos Oficina"
        V1["GET /veiculos-oficina/{providerId}"]
        V2["POST /adicionar-veiculo"]
        V3["PATCH /atualizar-status-veiculo/{id}"]
        V4["PUT /atualizar-veiculo/{id}"]
        V5["DELETE /excluir-veiculo/{id}/{providerId}"]
    end

    subgraph "🚙 Veículos Cliente"
        VC1["GET /veiculos-cliente/{customerId}"]
        VC2["POST /adicionar-veiculo-cliente"]
        VC3["PUT /atualizar-veiculo-cliente/{id}"]
        VC4["DELETE /excluir-veiculo-cliente/{id}/{customerId}"]
    end

    subgraph "⭐ Avaliação"
        AV1["POST /avaliar-pedido"]
    end

    subgraph "📡 WebSocket"
        WS1["WS /radar-provider/{id}"]
    end

    style A1 fill:#4CAF50,color:#fff
    style A2 fill:#4CAF50,color:#fff
    style A3 fill:#4CAF50,color:#fff
    style M1 fill:#f44336,color:#fff
    style M2 fill:#f44336,color:#fff
    style WS1 fill:#E91E63,color:#fff
```

---

## 7. Diagrama de Sequência — Fluxo de Match com WebSocket

```mermaid
sequenceDiagram
    participant C as 📱 App Cliente
    participant API as 🌐 API Ktor
    participant DB as 🗄️ MySQL
    participant WS as 📡 WebSocket
    participant O as 🔧 App Oficina

    Note over O,WS: Oficina já está conectada via WebSocket

    O->>WS: Conecta em /radar-provider/{id}
    WS->>WS: Armazena sessão no ConcurrentHashMap

    C->>API: POST /solicitar-socorro
    API->>DB: INSERT service_requests (searching)
    DB-->>API: request_id gerado

    API->>DB: SELECT oficinas próximas (ST_Distance_Sphere)
    DB-->>API: Lista de oficinas com distância

    API->>API: Calcula preço: base + (per_km × dist)

    API->>DB: INSERT service_matches (pending) [batch]
    
    API->>API: Inicia timer de 3 minutos (coroutine)
    
    API->>WS: Busca sessão da oficina no HashMap
    WS->>O: Frame.Text com JSON personalizado
    
    API-->>C: 201 Created + lista de matches

    Note over C: Cliente inicia polling

    loop Polling a cada X segundos
        C->>API: GET /status-pedido/{id}
        API->>DB: SELECT status FROM service_requests
        DB-->>API: status = "searching"
        API-->>C: {"status": "searching"}
    end

    O->>API: POST /aceitar-socorro
    API->>DB: UPDATE service_requests (accepted)
    API->>DB: UPDATE service_matches (accepted)
    API-->>O: {"sucesso": true}

    C->>API: GET /status-pedido/{id}
    API->>DB: SELECT + JOIN (oficina + veículo)
    DB-->>API: status = "accepted" + detalhes
    API-->>C: {"status": "accepted", "detalhesOficina": {...}}

    Note over C: Cliente vê detalhes da oficina

    O->>API: PATCH /atualizar-status-pedido/{id} (en_route)
    O->>API: PATCH /atualizar-status-pedido/{id} (arrived)
    O->>API: PATCH /atualizar-status-pedido/{id} (in_progress)
    O->>API: PATCH /atualizar-status-pedido/{id} (completed)

    C->>API: POST /avaliar-pedido
    API->>DB: INSERT service_reviews
    API-->>C: {"sucesso": true}
```

---

## 8. Fluxo de Dados — Routes → Repository → Database

```mermaid
flowchart LR
    subgraph "Routes (Controller)"
        AR["AuthRoutes.kt"]
        MR["MatchRoutes.kt"]
        PR["PedidosRoutes.kt"]
        PFR["PerfilRoutes.kt"]
        SR["ServicoRoutes.kt"]
        VR["VeiculoRoutes.kt"]
        WSR["WebSocketRoutes.kt"]
    end

    subgraph "Repository (DAO)"
        AREP["AuthRepository.kt<br/>• validarNoBanco<br/>• cadastrarNoBanco<br/>• solicitarRecuperacao"]
        MREP["MatchRepository.kt<br/>• solicitarSocorroRadar"]
        PREP["PedidosRepository.kt<br/>• buscarPedidos<br/>• buscarPedidosDoPrestador<br/>• buscarHistoricoDaOficina<br/>• verificarStatusDoPedidoBanco<br/>• aceitarPedidoBanco<br/>• atualizarStatusPedidoBanco"]
        PFREP["PerfilRepository.kt<br/>• buscarPerfilNoBanco<br/>• atualizarPerfilNoBanco<br/>• atualizarStatusOnline"]
        SREP["ServicoRepository.kt<br/>• adicionarServicoNoBanco<br/>• buscarServicosDaOficina<br/>• atualizarDadosServicoNoBanco<br/>• alternarStatusServicoNoBanco<br/>• excluirServicoNoBanco"]
        VREP["VeiculoRepository.kt<br/>• adicionarVeiculoNoBanco<br/>• buscarVeiculosDaOficina<br/>• atualizarStatusVeiculoNoBanco<br/>• excluirVeiculoNoBanco<br/>• atualizarDadosVeiculoNoBanco<br/>• CRUD veículo cliente<br/>• salvarAvaliacaoNoBanco"]
    end

    subgraph "Database"
        DC["DatabaseConfig<br/>(HikariCP)"]
        MySQL["MySQL"]
    end

    AR --> AREP
    MR --> MREP
    PR --> PREP
    PFR --> PFREP
    PFR --> SREP
    SR --> SREP
    VR --> VREP

    AREP --> DC
    MREP --> DC
    PREP --> DC
    PFREP --> DC
    SREP --> DC
    VREP --> DC
    DC --> MySQL

    style AR fill:#4CAF50,color:#fff
    style MR fill:#f44336,color:#fff
    style PR fill:#2196F3,color:#fff
    style PFR fill:#FF9800,color:#fff
    style SR fill:#9C27B0,color:#fff
    style VR fill:#00BCD4,color:#fff
    style WSR fill:#E91E63,color:#fff
    style MySQL fill:#795548,color:#fff
```

---

## 9. Fórmula do Radar — Cálculo de Preço e Distância

```mermaid
flowchart TD
    A["📍 Cliente: lat/lng"] --> B["🛰️ MySQL ST_Distance_Sphere()"]
    C["📍 Oficina: lat/lng"] --> B
    
    B --> D["distancia_metros"]
    D --> E["distancia_km = metros / 1000<br/>(arredondado 1 casa decimal)"]
    
    E --> F["minutos = max(2, distancia_km × 2.5)"]
    
    G["base_price<br/>(da tabela provider_services)"] --> H["preço_final = base_price +<br/>(price_per_km × distancia_km)"]
    I["price_per_km<br/>(da tabela provider_services)"] --> H
    E --> H

    H --> J["ProviderMatchDetail:<br/>providerId, preco, distanciaKm, minutosEstimados"]
    F --> J

    style B fill:#FF5722,color:#fff
    style H fill:#4CAF50,color:#fff
    style J fill:#2196F3,color:#fff
```

> **Exemplo prático:**  
> - `base_price` = R$ 50,00  
> - `price_per_km` = R$ 5,00  
> - `distância` = 7,3 km  
> - **Preço final** = 50 + (5 × 7,3) = **R$ 86,50**  
> - **Tempo estimado** = max(2, 7,3 × 2,5) = **~18 min**

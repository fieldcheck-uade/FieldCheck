# FieldCheck — Diagramas

Diagramas de la preentrega en Mermaid (se ven directamente en GitHub). Cada diagrama va acompañado de las decisiones de diseño que refleja.

1. [Flujo de pantallas del caso de uso principal](#1-flujo-de-pantallas-del-caso-de-uso-principal)
2. [Arquitectura: flujo de datos por capas](#2-arquitectura-flujo-de-datos-por-capas)
3. [Arquitectura: topología de infraestructura](#3-arquitectura-topología-de-infraestructura)
4. [Secuencia de sincronización](#4-secuencia-de-sincronización)
5. [Modelo de datos local (Room)](#5-modelo-de-datos-local-room)

---

## 1. Flujo de pantallas del caso de uso principal

Caso de uso: **realizar una inspección en campo y dejarla sincronizada**. Cubre RF01, RF02, RF03 y RF04.

```mermaid
flowchart TD
    start([Abrir la app]) --> P1["P1 · Historial"]

    P1 -->|"Tocar 'Nueva inspección'"| P2["P2 · Nueva inspección"]
    P1 -->|"Tocar una inspección finalizada"| P5["P5 · Detalle"]
    P1 -->|"Tocar una inspección en curso"| P3

    P2 --> D1{"¿Hay plantillas guardadas?"}
    D1 -->|No| D2{"¿Hay conexión?"}
    D2 -->|No| E1["Estado vacío: hace falta conexión<br/>la primera vez · 'Reintentar'"]
    E1 -->|Reintentar| D2
    E1 -->|Volver| P1
    D2 -->|Sí| DL["Descargar plantillas"] --> D1
    D1 -->|Sí| SEL["Elegir plantilla + ingresar sitio"]
    SEL -->|"Crear"| OP1[["Crear inspección en Room<br/>(copia los ítems de la plantilla)"]]
    OP1 --> P3["P3 · Ejecución ítem por ítem"]

    P3 -->|"Marcar Cumple / No cumple / N/A"| OP2[["Guardar al instante en Room"]]
    OP2 --> P3
    P3 -->|"Agregar foto"| CAM["Cámara del sistema"]
    CAM -->|"Foto confirmada"| OP3[["Guardar archivo interno<br/>+ registro de evidencia"]]
    CAM -->|"Cancelada"| P3
    OP3 --> P3
    P3 -->|"Último ítem o 'Revisar'"| P4["P4 · Revisión"]

    P4 -->|"Finalizar"| D3{"¿Todos los ítems con estado<br/>y todo 'No cumple' con evidencia?"}
    D3 -->|No| E2["Lista de faltantes"]
    E2 -->|"Tocar un faltante"| P3
    D3 -->|Sí| OP4[["Finalizar inspección<br/>+ encolar sincronización"]]
    OP4 --> P1b["P1 · Historial<br/>inspección 'Pendiente'"]

    P1b -. "segundo plano, al haber red" .-> D4{"¿Sincronizó?"}
    D4 -->|Sí| F1(["Fin: inspección 'Sincronizada'"])
    D4 -->|"Error permanente o<br/>se agotaron los reintentos"| E3["Estado 'Error' · 'Reintentar'"]
    E3 -->|Reintentar| D4

    P1b -->|"Abrir"| P5
    P5 -->|"Compartir"| SH["Hoja de compartir de Android"]
    SH --> F2(["Fin: resumen enviado"])
    P3 -->|"Salir o interrupción"| F3(["Fin parcial: queda 'En curso',<br/>se retoma en el primer ítem sin completar"])
```

**Decisiones de diseño**

- **Punto de entrada:** la app siempre abre en el Historial. Es la pantalla que responde "¿qué tengo pendiente?", que es lo primero que necesita el inspector.
- **Operaciones críticas** (doble borde): crear la inspección, guardar cada respuesta, guardar la foto y finalizar. Todas escriben en Room, ninguna depende de la red. Por eso ninguna tiene una rama "sin conexión".
- **Decisiones:** hay tres en el flujo del usuario. (1) Si hay plantillas guardadas; es el único punto donde la conectividad importa, y solo la primera vez. (2) La validación al finalizar, que aplica la regla de negocio del RF02. (3) El resultado de la sincronización, que ocurre en segundo plano (línea punteada) y el usuario no espera.
- **Finales posibles:** inspección sincronizada, resumen compartido, o inspección en curso interrumpida. Este último no es un error: está diseñado así (RNF02 y RNF05).
- **Prevención de errores:** la validación no muestra un mensaje genérico, lista los faltantes y cada uno lleva directo al ítem que hay que completar.
- **Cámara sin rama de permiso rechazado:** se usa la cámara del sistema mediante un intent (`TakePicture`), que no requiere el permiso `CAMERA`. El único permiso de la app es la ubicación (deseable). Si se rechaza, la foto se guarda igual sin coordenadas.

---

## 2. Arquitectura: flujo de datos por capas

```mermaid
flowchart LR
    subgraph PRES["Presentación (Android)"]
        direction TB
        UI["Pantallas Compose<br/>P1…P5"]
        VM["ViewModels<br/>StateFlow&lt;UiState&gt;"]
        UI -- "eventos del usuario" --> VM
        VM -- "UiState" --> UI
    end

    subgraph DOM["Dominio (Kotlin puro)"]
        direction TB
        UC["Casos de uso<br/>CreateInspection<br/>FinalizeInspection<br/>BuildInspectionSummary"]
        RI["Interfaces de repositorio<br/>InspectionRepository<br/>TemplateRepository"]
        MOD["Modelos<br/>Inspection · ItemResult<br/>Evidence · Template"]
        UC --> RI
    end

    subgraph DATA["Datos (Android)"]
        direction TB
        REPO["Implementaciones<br/>de repositorio"]
        MAP["Mappers<br/>Entity ↔ Dominio ↔ DTO"]
        LDS["Fuente local<br/>Room: DAOs + entidades<br/>+ archivos internos"]
        RDS["Fuente remota<br/>Retrofit: API + DTOs"]
        SYNC["SyncWorker<br/>(WorkManager)"]
        NET["NetworkMonitor<br/>Flow&lt;Boolean&gt;"]
        REPO --> MAP
        REPO --> LDS
        REPO --> RDS
        SYNC --> REPO
    end

    VM -- "acciones con regla de negocio" --> UC
    VM -- "lecturas simples (Flow)" --> RI
    REPO -. "implementa" .-> RI
    NET -- "hay conexión sí/no" --> VM
    LDS == "Flow: única fuente de verdad" ==> REPO
    RDS <--> API[("API REST")]
```

**Decisiones de diseño**

- **Dirección de las dependencias:** Presentación y Datos dependen de Dominio. Dominio no depende de nadie: no importa nada de Android, Room ni Retrofit. La flecha punteada "implementa" es la inversión de dependencias: el dominio define el contrato (`InspectionRepository`) y la capa de datos lo cumple. Por eso los casos de uso se testean con un repositorio falso, sin emulador.
- **Cómo circulan los datos, lectura:** Room emite un `Flow` → el repositorio lo mapea a modelos de dominio → el ViewModel lo transforma en `UiState` → Compose lo dibuja. Cuando cambia algo en Room, la pantalla se actualiza sola. La UI **nunca** lee de la red (línea gruesa: única fuente de verdad).
- **Cómo circulan los datos, escritura:** evento de la UI → ViewModel → caso de uso (si hay regla) → repositorio → Room. El `Flow` anterior propaga el cambio a la pantalla.
- **Red:** solo la usan el `SyncWorker` (para enviar inspecciones) y el repositorio de plantillas (para refrescar la caché). Lo que traen de la red lo escriben en Room, nunca lo pasan directo a la UI.
- **Por qué no todo pasa por un caso de uso:** listar el historial es una lectura sin reglas. Un `GetInspectionsUseCase` que solo llama al repositorio sería una capa vacía. Los casos de uso existen donde hay lógica: copiar los ítems de la plantilla, validar la finalización y armar el resumen.
- **`NetworkMonitor`:** solo alimenta el banner offline de la UI. La decisión de cuándo sincronizar es de WorkManager, con su restricción de red.
- **Mappers:** separan tres formas del mismo dato. La entidad de Room está optimizada para la base, el DTO sigue el formato JSON de la API y el modelo de dominio es el que usa la app. Si cambia la API, cambian el DTO y el mapper, no la UI.

---

## 3. Arquitectura: topología de infraestructura

```mermaid
flowchart LR
    subgraph DEV["Dispositivo Android (API 26+)"]
        direction TB
        APP["App FieldCheck<br/>(proceso de la app)"]
        DB[("Room · SQLite<br/>fieldcheck.db")]
        FILES[/"Almacenamiento interno<br/>files/evidence/*.jpg"/]
        WM["WorkManager<br/>(persiste los trabajos,<br/>sobrevive reinicios)"]
        CAMAPP["App de cámara<br/>del sistema"]
        LOC["Servicios de ubicación<br/>(opcional)"]
        SHARE["Hoja de compartir<br/>→ mail, WhatsApp, etc."]
        APP --> DB
        APP --> FILES
        APP -- "encola trabajo" --> WM
        WM -- "ejecuta SyncWorker<br/>cuando hay red" --> APP
        APP -- "TakePicture +<br/>FileProvider" --> CAMAPP
        CAMAPP -- "escribe la foto" --> FILES
        APP -. "lectura puntual" .-> LOC
        APP -- "ACTION_SEND" --> SHARE
    end

    NETW{{"Red móvil / Wi-Fi<br/>(intermitente)"}}

    subgraph SRV["Servidor (Docker Compose)"]
        direction TB
        API["Contenedor api<br/>FastAPI + Uvicorn :8000"]
        SDB[("SQLite<br/>(volumen)")]
        SFILES[/"Fotos recibidas<br/>(volumen)"/]
        API --> SDB
        API --> SFILES
    end

    APP <== "HTTP(S) + header X-API-Key<br/>GET /templates<br/>POST /inspections<br/>POST /inspections/{client_uuid}/evidence" ==> NETW
    NETW <==> API
```

**Decisiones de diseño**

- **Qué corre dónde:** en el dispositivo están la app, su base Room, las fotos en almacenamiento interno y WorkManager. En el servidor hay un solo contenedor con FastAPI, que guarda los datos en SQLite y las fotos en disco. Ambos van en volúmenes de Docker para que no se pierdan al recrear el contenedor.
- **La red es el eslabón débil** (hexágono): el diseño asume que está cortada la mayor parte del tiempo en campo. Todo lo que está a la izquierda funciona sin ella.
- **WorkManager aparece como componente propio** porque tiene su propia persistencia: si el teléfono se reinicia con una inspección pendiente, el trabajo sigue encolado y se ejecuta cuando vuelve la red, sin abrir la app.
- **La cámara es otra app:** la nuestra le presta un archivo propio mediante `FileProvider` (una URI temporal con permiso de escritura) y la cámara escribe ahí. La foto nunca pasa por la galería (RNF08).
- **Entorno de desarrollo:** el servidor corre en la notebook con `docker compose up`. Desde el emulador, la notebook se ve como `10.0.2.2` (no `localhost`, que sería el propio emulador). En local se usa HTTP permitido solo para ese host en debug; un despliegue en la nube usaría HTTPS.
- **Seguridad:** la API key viaja en un header y se configura fuera del código fuente (`local.properties` → `BuildConfig`). Es una protección mínima acorde a una v1 sin usuarios; lo reconocemos como limitación.
- **SQLite en el servidor:** hay un solo escritor lógico por inspección y el volumen de datos de esta versión es bajo. Postgres agregaría otro contenedor sin aportar beneficios en esta etapa.

---

## 4. Secuencia de sincronización

Escenario: el inspector finaliza sin señal, la red vuelve más tarde y, en el primer intento, se corta la conexión justo después de que el servidor guardó los datos.

```mermaid
sequenceDiagram
    autonumber
    actor U as Inspector
    participant VM as ViewModel
    participant UC as FinalizeInspectionUseCase
    participant R as InspectionRepository
    participant DB as Room
    participant WM as WorkManager
    participant W as SyncWorker
    participant API as API (FastAPI)

    Note over U,API: Sin conexión
    U->>VM: Tocar "Finalizar"
    VM->>UC: finalize(inspectionId)
    UC->>R: obtener inspección e ítems
    R->>DB: consulta
    DB-->>UC: inspección completa
    UC->>UC: validar (estados + evidencia en "No cumple")
    UC->>R: marcar FINALIZADA, syncStatus = PENDING
    R->>DB: update
    UC->>WM: enqueueUniqueWork("fieldcheck-sync", APPEND_OR_REPLACE)<br/>restricción: red conectada, backoff exponencial
    DB-->>VM: Flow emite cambio
    VM-->>U: Historial: "Pendiente"
    Note over WM: El trabajo espera: no se cumple la restricción de red

    Note over U,API: Recupera conexión
    WM->>W: doWork()
    W->>R: syncInspection(clientUuid)
    R->>API: POST /inspections (clientUuid, ítems, updatedAt)
    API->>API: upsert por client_uuid (last-write-wins por updatedAt)
    API--xR: la conexión se corta antes de la respuesta
    R-->>W: IOException (error transitorio)
    W-->>WM: Result.retry()
    Note over WM: Espera exponencial y nuevo intento cuando hay red

    WM->>W: doWork() (intento 2)
    W->>R: syncInspection(clientUuid)
    R->>API: POST /inspections (mismo clientUuid)
    API->>API: ya existe: actualiza, no duplica
    API-->>R: 200 OK
    loop Por cada evidencia con syncStatus PENDING o FAILED
        R->>API: POST /inspections/{clientUuid}/evidence (multipart, evidence clientUuid)
        API-->>R: 201 Created / 200 si ya existía
        R->>DB: evidencia syncStatus = SYNCED
    end
    R->>DB: inspección syncStatus = SYNCED
    W-->>WM: Result.success()
    DB-->>VM: Flow emite cambio
    VM-->>U: Historial: "Sincronizada"

    Note over U,API: Alternativa: error permanente
    alt 401 / 422, o se agotaron los reintentos
        R->>DB: syncStatus = FAILED + motivo
        W-->>WM: Result.failure()
        DB-->>VM: Flow emite cambio
        VM-->>U: "Error: motivo" + "Reintentar"
        U->>VM: Tocar "Reintentar"
        VM->>WM: encolar de nuevo el mismo trabajo único
    end
```

**Decisiones de diseño**

- **Con conexión:** el mismo flujo, solo que el trabajo se ejecuta enseguida después de encolarse. No hay un camino especial "online": un solo camino es más fácil de probar.
- **Pérdida de conexión:** finalizar nunca falla por falta de red, porque solo escribe en Room y encola. El usuario ve "Pendiente" al instante (pasos 1–10).
- **Recuperación:** WorkManager despierta el worker cuando se cumple la restricción de red, aunque la app esté cerrada.
- **El caso difícil (pasos 14–17):** el servidor guardó los datos pero el teléfono no recibió la respuesta. Sin idempotencia, el reintento crearía una inspección duplicada. Con el `clientUuid` generado en el dispositivo, el servidor reconoce que ya la tiene y actualiza.
- **Orden: primero datos, después fotos.** Las fotos referencian a la inspección por su `clientUuid`, así que la inspección tiene que existir primero. Además, si se corta a mitad de las fotos, las que ya subieron quedan `SYNCED` y el reintento solo sube las que faltan.
- **Transitorio vs. permanente:** un timeout o un 5xx se reintentan solos (`Result.retry()`); un 401 o un 422 no se arreglan reintentando, así que pasan a `FAILED` y el usuario decide.
- **Un único trabajo de sincronización (`APPEND_OR_REPLACE`):** un solo `SyncWorker` recorre todas las inspecciones pendientes en orden, así nunca hay dos envíos en paralelo. Si se finaliza otra inspección mientras corre, el nuevo pedido se encola detrás. (En la preentrega era un trabajo por inspección con `KEEP`; ver el README, "Cambios respecto de la preentrega").
- **Conflictos:** después de finalizar, la inspección es de solo lectura en la app, y cada inspección tiene un único dueño. El *last-write-wins* del servidor queda como red de seguridad, no como mecanismo principal.

---

## 5. Modelo de datos local (Room)

```mermaid
erDiagram
    TEMPLATE ||--|{ TEMPLATE_ITEM : "tiene"
    TEMPLATE ||--o{ INSPECTION : "origina"
    INSPECTION ||--|{ ITEM_RESULT : "contiene"
    ITEM_RESULT ||--o{ EVIDENCE : "respalda"

    TEMPLATE {
        string id PK "id del servidor"
        string name
        int version
        long downloadedAt "para mostrar antigüedad de la caché"
    }
    TEMPLATE_ITEM {
        string id PK
        string templateId FK
        string text
        int position
    }
    INSPECTION {
        string clientUuid PK "generado en el dispositivo"
        string templateId FK
        string templateName "copia"
        string siteName
        string status "IN_PROGRESS | FINISHED"
        long startedAt
        long finishedAt "nullable"
        string syncStatus "PENDING | SYNCED | FAILED"
        string lastSyncError "nullable"
        long updatedAt
    }
    ITEM_RESULT {
        long id PK "autogenerado"
        string inspectionId FK
        string itemText "copia del texto de la plantilla"
        int position
        string result "COMPLIES | NOT_COMPLIES | NOT_APPLICABLE | nullable"
        string note "nullable"
        long updatedAt
    }
    EVIDENCE {
        string clientUuid PK "generado en el dispositivo"
        long itemResultId FK
        string filePath "ruta en almacenamiento interno"
        long capturedAt
        double latitude "nullable"
        double longitude "nullable"
        string syncStatus "PENDING | SYNCED | FAILED"
    }
```

**Decisiones de diseño**

- **Dos grupos de datos:** `TEMPLATE` y `TEMPLATE_ITEM` vienen de la API y son una caché que se puede reemplazar entera. `INSPECTION`, `ITEM_RESULT` y `EVIDENCE` los genera el usuario, solo existen en el dispositivo hasta que se sincronizan y nunca se pisan con datos del servidor.
- **Copia del texto del ítem (`itemText`, `templateName`):** es una decisión deliberada de desnormalización. Una inspección es un registro histórico: si mañana la plantilla cambia, la inspección de hoy tiene que seguir mostrando lo que se revisó. Por eso `ITEM_RESULT` no depende de `TEMPLATE_ITEM` después de creada.
- **Claves generadas en el dispositivo (`clientUuid`):** permiten crear registros sin conexión, sin esperar un id del servidor, y son la base de la idempotencia de la sincronización (diagrama 4).
- **`syncStatus` como cola (patrón *outbox*):** el worker consulta "todo lo finalizado que esté `PENDING`". Las `FAILED` no se toman solas: vuelven a `PENDING` cuando el usuario toca "Reintentar". No hace falta una tabla de cola aparte, porque el estado vive junto al dato que describe.
- **`syncStatus` en `EVIDENCE` además de en `INSPECTION`:** las fotos son lo más pesado y lo que más probablemente falle con señal débil. Llevar el estado por foto permite reanudar donde se cortó.
- **Las fotos no se guardan en la base:** Room guarda la ruta, el archivo vive en `files/evidence/`. Guardar blobs en SQLite agranda la base y empeora las consultas.
- **`result` nullable:** `null` significa "sin completar". Es lo que permite calcular el progreso ("12 de 20") y validar la finalización.
- **Borrado en cascada:** `INSPECTION → ITEM_RESULT → EVIDENCE` se define con `onDelete = CASCADE`. Si en el futuro se borra una inspección, no quedan huérfanos (los archivos de las fotos se borran desde el repositorio).

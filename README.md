# FieldCheck

App Android para hacer **inspecciones de campo guiadas por checklist**. Por cada ítem se registra un resultado (Cumple / No cumple / N/A), una nota y fotos como evidencia. Funciona **100 % sin conexión** y se sincroniza sola con un servidor cuando vuelve la señal.

Trabajo Práctico Obligatorio de **Desarrollo de Aplicaciones I** (UADE).

> **Estado actual:** Etapa 2, entrega final de implementación. Los cuatro requisitos funcionales comprometidos (RF01 a RF04) están implementados y probados de punta a punta en un emulador contra el backend.
>
> La documentación de la Etapa 1 (preentrega aprobada) está en [`docs/preentrega.md`](docs/preentrega.md). Lo que cambió desde entonces se explica en [Cambios respecto de la preentrega](#cambios-respecto-de-la-preentrega).

---

## Qué problema resuelve

Quien hace inspecciones en obra, planta o salas técnicas trabaja en lugares con mala señal. Hoy junta fotos en la galería y notas en papel o en WhatsApp, y después reconstruye todo a mano en un Excel. Esa reconstrucción lleva tiempo, pierde evidencia y no deja claro qué foto corresponde a qué observación.

FieldCheck permite registrar la inspección **una sola vez, en el lugar**, con la evidencia asociada a cada ítem. El informe llega al servidor apenas vuelve la conexión, sin que el inspector tenga que acordarse de mandarlo.

**Problema → Usuario → Solución → Valor:** evidencia dispersa y reconstruida a mano → inspector de campo con conectividad intermitente → checklist guiado, offline, con fotos por ítem y sincronización automática → menos retrabajo, evidencia trazable y el informe disponible en cuanto hay señal.

Detalle completo en [`docs/preentrega.md`](docs/preentrega.md), puntos 2 a 7.

## Funcionalidades implementadas

| Pantalla | Qué permite hacer |
|---|---|
| **P1 · Historial** | Ver las inspecciones en curso y finalizadas, con su progreso ("3 de 12 ítems") y su estado de sincronización (Pendiente / Sincronizada / Error). Tocar una en curso la retoma en el primer ítem sin completar; tocar una finalizada abre su detalle. Botón de modo oscuro. |
| **P2 · Nueva inspección** | Elegir una plantilla de la caché (con la antigüedad de la última descarga), escribir el sitio y empezar. Funciona sin conexión. |
| **P3 · Ejecución** | Recorrer los ítems de a uno: marcar Cumple / No cumple / N/A con un toque, escribir una nota y sacar fotos con la cámara del sistema. Cada cambio se guarda al instante. |
| **P4 · Revisión y finalización** | Ver el resumen y los ítems que faltan. No deja finalizar si hay ítems sin resultado o un "No cumple" sin nota ni foto, y lleva directo al ítem que falta. |
| **P5 · Detalle** | Ver la inspección finalizada (solo lectura) con sus fotos, el estado de sincronización y el motivo si falló, con un botón "Reintentar". Compartir un resumen en texto con cualquier app. |
| **Sincronización** | Al finalizar, la inspección queda "Pendiente" y se sube sola (datos y fotos) en cuanto hay red, aunque la app esté cerrada. Las plantillas también se actualizan desde el servidor. |

Cada pantalla contempla los estados que pide la consigna (7.3): **carga** (esqueletos), **contenido**, **vacío** (con la acción para salir de él), **error** (qué pasó y "Reintentar") y **sin conexión** (banner que no bloquea nada, porque todo funciona offline). La app no pide permisos peligrosos: la cámara la maneja la app de cámara del sistema. Si el teléfono no tiene una, se avisa en lugar de cerrarse.

## Requisitos funcionales alcanzados

| Requisito | Descripción | Estado | Cómo se verificó |
|---|---|---|---|
| RF01 | Crear una inspección a partir de una plantilla (plantillas cacheadas) | ✅ Listo | En modo avión, la inspección se crea con los ítems de la plantilla y sigue ahí al reabrir la app. Las plantillas se descargan de la API cuando hay red. |
| RF02 | Completar ítems con resultado, nota y foto, sin conexión | ✅ Listo | En modo avión se completan los ítems con nota y foto, y se verifica la regla de finalización (tests de `CheckFinalizationUseCase` y `FinalizeInspectionUseCase`). |
| RF03 | Sincronización automática con estado visible y reintento | ✅ Listo | Una inspección finalizada en modo avión queda "Pendiente". Al sacar el modo avión pasa sola a "Sincronizada" y en el servidor aparece una vez, con sus fotos. Con una API key inválida queda en "Error" con el motivo, y "Reintentar" la sube. |
| RF04 | Historial, detalle y compartir resumen | ✅ Listo | El historial y el detalle funcionan en modo avión. "Compartir" abre la hoja de Android con el texto del resumen (test de `BuildInspectionSummaryUseCase`). |

**Requisitos no funcionales** (definidos en `docs/preentrega.md`, punto 10):

| RNF | Estado |
|---|---|
| RNF01 Offline | ✅ Crear, completar, consultar y compartir funcionan en modo avión. |
| RNF02 Confiabilidad | ✅ Cada cambio se escribe en Room al instante y la escritura termina aunque se cierre la pantalla. |
| RNF03 Sincronización idempotente con reintentos | ✅ Idempotencia por `clientUuid` en el servidor. Errores transitorios con espera exponencial; los permanentes no se reintentan solos. Hay tests con MockWebServer. |
| RNF04 Recuperación ante errores | ✅ Todo error visible dice qué pasó y ofrece una acción ("Reintentar"). Los errores de red no cierran la app. |
| RNF05 Uso con una mano | ✅ Las acciones principales están en la zona del pulgar, un toque por resultado y se retoma en el primer ítem sin completar. |
| RNF06 Accesibilidad | ⚠️ Implementado: íconos con descripción, estado con ícono + texto, áreas táctiles de 48 dp. **Falta** la pasada con Accessibility Scanner y TalkBack. |
| RNF07 Fotos redimensionadas | ✅ Al guardarla, cada foto se reduce a 1600 px de lado mayor (JPEG calidad 80), respetando la orientación. Verificado en el emulador: una foto de 1440 × 1920 se guarda y se sube en 1200 × 1600. |
| RNF08 Privacidad y seguridad | ✅ Las fotos quedan en el almacenamiento interno, sin permisos de almacenamiento ni backup automático. La API key está fuera del código y el log de red existe solo en debug. |
| RNF09 Batería | ✅ La sincronización corre solo con red. La ubicación no se implementó, así que no se usan sensores. |
| RNF10 Compatibilidad | ⚠️ `minSdk 26`. Probado en un emulador con la última API; **falta** probar en API 26. |
| RNF11 Testabilidad | ✅ El dominio no importa Android. Todos los casos de uso y ViewModels tienen tests (`./gradlew testDebugUnitTest`). |

## Instrucciones de ejecución

### Requisitos

- Android Studio (versión estable reciente), JDK 17 o superior y el SDK de Android 37.
- Docker, para el backend.
- Un emulador o teléfono con Android 8.0 (API 26) o superior.

### 1. Levantar el backend

```bash
cd backend
docker compose up --build
```

La API queda en `http://localhost:8000` (documentación interactiva en `http://localhost:8000/docs`). Al arrancar carga tres plantillas de ejemplo. La configuración, los endpoints y cómo correrla sin Docker están en [`backend/README.md`](backend/README.md).

### 2. Ejecutar la app

1. Abrir la carpeta raíz del repo en Android Studio y esperar a que Gradle sincronice.
2. Ejecutar la configuración `app` en un emulador.

La app de debug ya apunta al backend local tal como lo ve el emulador (`http://10.0.2.2:8000/`) y usa la API key de desarrollo, así que no hace falta configurar nada.

La primera vez, la app trae cuatro plantillas de ejemplo para poder usarla aunque nunca haya tenido conexión. Apenas hay red y el backend responde, se reemplazan por las del servidor.

**Otra URL o API key.** Agregar en `local.properties` (no se sube al repo):

```properties
fieldcheck.apiBaseUrl=http://10.0.2.2:8000/
fieldcheck.apiKey=dev-api-key
```

En debug, la app solo permite HTTP sin cifrar hacia `10.0.2.2` y `localhost` (`app/src/debug/res/xml/network_security_config.xml`). Para usar un teléfono físico hay que agregar ahí la IP de la computadora y usarla en `fieldcheck.apiBaseUrl`.

### 3. Tests

```bash
./gradlew testDebugUnitTest            # app: 152 tests unitarios, sin emulador
cd backend && python -m pytest         # backend: 15 tests (ver backend/README.md)
```

### Probar el flujo principal (guion de la demo)

1. Con el backend levantado, abrir la app: en "Nueva inspección" aparecen las plantillas del servidor.
2. Activar el **modo avión** en el emulador. Aparece el banner "Sin conexión" y todo sigue funcionando.
3. Crear una inspección. Marcar un ítem "No cumple" e intentar finalizar: la app no deja y lleva al ítem. Agregarle una nota o una foto.
4. Completar el resto y finalizar. En el detalle se ve **"Pendiente de subir"**.
5. Desactivar el modo avión. Sin tocar nada, pasa a **"Sincronizada"**. En `http://localhost:8000/docs` → `GET /inspections/{client_uuid}` se ven los ítems y las fotos.
6. Tocar "Compartir" para mandar el resumen por cualquier app.

## Arquitectura final

**MVVM + Clean Architecture**, con inyección de dependencias con **Hilt** y UI en **Jetpack Compose**. Toma como referencia la arquitectura de [PokedexAcademy-MVVM-Clean](https://github.com/ExBattou/PokedexAcademy-MVVM-Clean), vista en clase.

```
Pantalla (Compose) → ViewModel → Caso de uso → Repositorio (interfaz) → Room (local) / Retrofit (remoto)
     ↑ UiState (StateFlow)          ↑ Resource (Success / Error)
                                                     WorkManager (SyncWorker) → Caso de uso → Repositorio
```

| Capa | Qué hace | Qué NO hace |
|---|---|---|
| **Presentation** | Dibuja la pantalla (Compose) y guarda su estado (ViewModel). | No sabe de dónde vienen los datos. |
| **Domain** | Modelos, reglas de negocio (casos de uso) e interfaces de repositorio. Kotlin puro. | No importa nada de Android, Room ni Retrofit. |
| **Data** | Implementa los repositorios con Room y Retrofit, y programa la sincronización con WorkManager. Convierte entidades, DTOs y modelos con mappers. | No tiene reglas de negocio. |
| **DI** | Módulos de Hilt que conectan cada interfaz con su implementación. | — |

**Cómo viaja un dato (ejemplo: Historial, P1)**

1. Room emite la lista de inspecciones como `Flow` (`InspectionDao`).
2. `InspectionRepositoryImpl` la convierte a modelos de dominio y la envuelve en `Resource.Success`. Si la base falla, emite `Resource.Error` con un mensaje entendible.
3. `GetInspectionHistoryUseCase` aplica la regla de negocio: separa "En curso" de "Finalizadas" y las ordena.
4. `HistoryViewModel` lo traduce a `UiState` (`Loading`, `Success` o `Error`) y lo expone en un `StateFlow`.
5. `HistoryScreen` dibuja cada estado. Los eventos de una sola vez (navegar, snackbars) viajan aparte, por un `Channel`, para que no se repitan al rotar la pantalla ni se pierdan si la pantalla no está escuchando en ese momento.

**Cómo se sincroniza una inspección (RF03)**

1. Al finalizar (o al tocar "Reintentar" en el detalle), el caso de uso deja la inspección en `PENDING` y pide una sincronización a `SyncScheduler`.
2. `WorkManagerSyncScheduler` encola `SyncWorker` con la condición "hay red". Sin conexión, WorkManager espera, aunque la app esté cerrada.
3. Con red, `SyncWorker` actualiza las plantillas y ejecuta `SyncPendingInspectionsUseCase`, que sube cada inspección pendiente con `SyncRepository`: primero los datos y después las fotos que falten.
4. Según el resultado:
   - Si todo sube, la inspección pasa a "Sincronizada".
   - Ante un error transitorio (sin red, timeout o 5xx), sigue "Pendiente" y WorkManager reintenta con espera exponencial.
   - Ante un error permanente (4xx, por ejemplo una API key inválida), pasa a "Error" con el motivo y espera a que el usuario toque "Reintentar".
5. La UI no se entera de la red: lee el estado desde Room, así que el chip del historial y la tarjeta del detalle se actualizan solos.

**Offline first:** Room es la única fuente de verdad de la UI. Con conexión, sin conexión o recuperándola, la pantalla muestra lo mismo; lo único que cambia es el banner y el momento en que los datos llegan al servidor. Diagramas de datos, infraestructura y secuencia de sincronización en [`docs/diagramas.md`](docs/diagramas.md).

**Estructura de paquetes**

```
app/src/main/java/ar/edu/uade/fieldcheck/
├── FieldCheckApplication.kt   @HiltAndroidApp: arranca Hilt, configura WorkManager y pide la primera sincronización
├── MainActivity.kt            @AndroidEntryPoint: única Activity, contiene la UI Compose
├── core/                      lo compartido por todas las capas
│   ├── model/                 Resource (resultado de Data) y UiState (estado de pantalla)
│   ├── navigation/            rutas tipadas (Screen)
│   └── theme/                 colores, tipografía y formas (docs/pantallas.md)
├── domain/                    Kotlin puro, sin Android
│   ├── model/                 Inspection, ItemResult, Template, SyncResult...
│   ├── repository/            interfaces: InspectionRepository, TemplateRepository, SyncRepository, SyncScheduler...
│   └── usecase/               casos de uso con reglas de negocio
├── data/
│   ├── local/                 Room: database (con migraciones), dao, entity; archivos de fotos
│   ├── remote/                Retrofit: api, dto y FieldCheckRemoteDataSource (clasifica errores)
│   ├── connectivity/          monitor de red como Flow (banner "Sin conexión")
│   ├── mapper/                Entity ↔ Dominio y Entity ↔ DTO
│   ├── repository/            implementaciones de los repositorios
│   └── sync/                  SyncWorker y WorkManagerSyncScheduler (WorkManager)
├── di/                        módulos de Hilt (Database, DataStore, Network, Repository, Time)
└── presentation/
    ├── FieldCheckApp.kt       NavHost con el grafo de navegación
    ├── ThemeViewModel.kt      modo claro / oscuro de toda la app (guardado en DataStore)
    ├── common/components/     componentes compartidos (chips, banner offline, estados vacío/error/carga)
    ├── history/               P1 · Historial
    ├── newinspection/         P2 · Nueva inspección
    ├── execution/             P3 · Ejecución de la inspección (cámara incluida)
    ├── review/                P4 · Revisión y finalización
    └── detail/                P5 · Detalle y compartir resumen
app/src/test/                  tests unitarios con repositorios falsos y MockWebServer (sin emulador)
app/schemas/                   esquemas exportados de Room (versiones 1 y 2)
backend/                       API FastAPI + SQLite (ver backend/README.md)
docs/                          documentación de la preentrega, diagramas y diseño
```

**Cómo agregar una pantalla nueva**

1. Si hace falta, el modelo en `domain/model` y el método en la interfaz del repositorio.
2. Un caso de uso en `domain/usecase` con la regla de negocio.
3. La implementación en `data/repository` (Hilt ya la conoce por `RepositoryModule`).
4. Un `XxxViewModel` con `@HiltViewModel` que exponga `StateFlow<UiState<...>>`.
5. Un `XxxRoute` (se conecta al ViewModel) y un `XxxScreen` (solo dibuja), y la ruta en `Screen` + `FieldCheckApp`.

## Tecnologías utilizadas

| Área | Tecnología | Para qué |
|---|---|---|
| Lenguaje y UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09), Material 3 | UI declarativa: cada estado de pantalla se dibuja a partir de un único objeto de estado. |
| Navegación y estado | Navigation Compose (rutas tipadas), ViewModel, StateFlow, Coroutines | Pasar ids entre pantallas con chequeo en compilación y mantener el estado ante rotaciones. |
| Inyección de dependencias | Hilt (Dagger) | Una instancia de cada dependencia, entregada a ViewModels, repositorios y workers. |
| Persistencia | Room 2.8 (KSP); DataStore Preferences | Room: inspecciones, ítems, fotos y caché de plantillas, con relaciones y consultas observables. DataStore: solo la preferencia de modo oscuro. |
| Red | Retrofit 3, OkHttp 5, kotlinx.serialization | Consumir la API (JSON y fotos multipart). Log de red solo en debug. |
| Segundo plano | WorkManager + `hilt-work` | Sincronizar cuando hay red, aunque la app esté cerrada, con reintentos y espera exponencial. |
| Dispositivo | Cámara del sistema (`TakePicture` + `FileProvider`), `ConnectivityManager`, hoja de compartir (`ACTION_SEND`) | Fotos sin permiso `CAMERA` y sin pasar por la galería; banner offline; compartir el resumen con cualquier app. |
| Imágenes | Coil 3 | Miniaturas de las fotos locales sin problemas de memoria. |
| Tests | JUnit4, Turbine, kotlinx-coroutines-test, MockWebServer | Casos de uso, ViewModels y cliente de la API, con repositorios falsos escritos a mano. |
| Backend | FastAPI, SQLAlchemy, SQLite, Docker Compose | Fuente remota propia, para controlar la idempotencia y probar los casos de error. |

Justificación de cada decisión y alternativas descartadas en `docs/preentrega.md`, punto 15. Lo que cambió está en la sección siguiente.

## Decisiones relevantes

- **Room como única fuente de verdad:** la UI nunca lee de la red, así que funciona igual con o sin conexión.
- **Sincronización al finalizar:** una inspección finalizada es de solo lectura, lo que casi elimina los conflictos. Ante un conflicto, el servidor aplica *last-write-wins*.
- **Idempotencia por `clientUuid`:** el teléfono genera el id de cada inspección y de cada foto. Reintentar un envío nunca duplica datos en el servidor.
- **Errores transitorios y permanentes:** solo se reintenta automáticamente lo que puede andar más tarde (sin red, timeout, 5xx). Un 4xx no se arregla reintentando, así que se muestra con su motivo y queda en manos del usuario.
- **Fotos marcadas de a una:** cada foto subida se marca como sincronizada, así un corte a mitad de la subida no obliga a mandar todas otra vez.
- **Regla de finalización en el dominio:** "No cumple" exige nota o foto, y se valida en el caso de uso, no en la pantalla. Así la regla no depende de por dónde se llegue a finalizar.
- **Cámara del sistema en lugar de CameraX:** menos código y sin pedir el permiso `CAMERA`.
- **Fotos reducidas al guardarlas, no al subirlas:** la foto se achica una sola vez, apenas vuelve de la cámara. Así ocupa menos lugar en el teléfono y cada reintento de subida manda el archivo chico. La regla está en `AddEvidenceUseCase`, y el procesamiento de la imagen (decodificar submuestreada, escalar, girar según el EXIF) en `EvidenceFileStore`.
- **API key fuera del código:** se lee de `local.properties` en tiempo de compilación (`BuildConfig`) y el log de red la oculta.
- **Migración de Room 1 → 2:** se agregó `templateVersion` a las inspecciones (la pide la API) con una migración en lugar de borrar la base, para no perder inspecciones guardadas.
- **Casos de uso entre ViewModel y repositorio:** los ViewModels hablan siempre con casos de uso y nunca con repositorios, como en la arquitectura de referencia (PokedexAcademy). La mayoría tiene una regla de negocio concreta (por ejemplo `FinalizeInspectionUseCase`, `CreateInspectionUseCase` o `GetInspectionHistoryUseCase`). Algunos son simples a propósito y solo llaman al repositorio (`GetInspectionDetailUseCase`, `DeleteEvidenceUseCase`, `ObserveDarkModeUseCase`, entre otros): se mantienen por consistencia, para que haya una sola regla ("el ViewModel usa casos de uso") y para que, si mañana aparece una regla, tenga un lugar donde ir sin tocar el ViewModel.

## Cambios respecto de la preentrega

La consigna permite cambios durante el desarrollo siempre que se puedan justificar. Estos son los que hicimos:

| Qué decía la preentrega | Qué se hizo | Por qué |
|---|---|---|
| Inyección de dependencias manual con un `AppContainer`; Hilt descartado. | **Hilt.** | Es la arquitectura vista en clase (PokedexAcademy). Además, `SyncWorker` necesita recibir casos de uso y Hilt lo resuelve con `hilt-work`; con un contenedor manual había que escribir una fábrica de workers a mano. |
| DataStore descartado (no aportaba). | **DataStore** solo para el modo oscuro. | Apareció una preferencia del usuario (modo oscuro) que no es un dato relacional. Ponerla en Room habría sido forzar una tabla para un solo valor. |
| Modo oscuro que sigue la configuración del sistema. | El usuario lo elige con un botón y se recuerda entre aperturas. | Para usar en interiores con poca luz sin cambiar la configuración de todo el teléfono. Criterio A9 actualizado en `docs/pantallas.md`. |
| WorkManager con un trabajo único **por inspección** (`KEEP`). | Un trabajo único de sincronización para todas las pendientes (`APPEND_OR_REPLACE`). | Con un trabajo por inspección, varios envíos podían correr en paralelo con señal débil. Un solo trabajo recorre las pendientes en orden, también actualiza las plantillas, y si llega una inspección nueva mientras corre, se encola detrás. La idempotencia sigue en el servidor. |
| `templateVersion` no figuraba en la inspección local. | Columna nueva con **migración de Room 1 → 2**. | La API la pide para saber con qué versión de la plantilla se hizo cada inspección. |
| Tests con MockK y Room in-memory. | Repositorios **falsos escritos a mano** y MockWebServer. Sin tests de Room in-memory. | Los fakes alcanzan para casos de uso y ViewModels y son más fáciles de leer y explicar. Room in-memory necesita tests instrumentados (emulador), y las consultas se verificaron de punta a punta. |
| Geoetiquetado de fotos (deseable). | **No se implementó.** | Era opcional y se priorizó completar la sincronización. El modelo ya tiene latitud y longitud, y la API las acepta. |

## Limitaciones conocidas

- **Sin ubicación en las fotos:** el geoetiquetado (deseable) quedó afuera.
- **Sin login ni multiusuario:** la app se autentica ante la API con una sola API key compartida.
- **Solo para desarrollo:** no hay una URL de producción con HTTPS ni firma de release. La app de debug habla HTTP solo con el backend local.
- **Plantillas de ejemplo distintas a las del servidor:** las cuatro de la primera apertura sin conexión no son las mismas que las tres del backend. Al primer contacto con el servidor se reemplazan, y las inspecciones ya creadas no se ven afectadas porque guardan su propia copia de los textos.
- **Las plantillas se administran en el servidor** y no se editan desde la app.
- **No hay exportación a PDF:** el resumen se comparte como texto, sin fotos adjuntas.
- **Una inspección finalizada no se puede editar.**
- **Verificaciones manuales pendientes:** Accessibility Scanner y TalkBack (RNF06), y la prueba en un emulador con API 26 (RNF10).

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/preentrega.md`](docs/preentrega.md) | Los 17 entregables de la preentrega: problema, usuario, requisitos, offline first, tecnologías, roles. |
| [`docs/diagramas.md`](docs/diagramas.md) | Flujo de pantallas, arquitectura (datos e infraestructura), secuencia de sincronización y modelo de datos. |
| [Figma](https://www.figma.com/design/huvTo8nDgobJskL3ysZDSj/FieldCheck-%E2%80%94-Mockups--DevApps1-) | Wireframes y mockups de alta fidelidad con los estados de cada pantalla (exportados en [`docs/diseno/`](docs/diseno/)). |
| [`docs/pantallas.md`](docs/pantallas.md) | Especificación de pantallas y estados, y criterios de accesibilidad. |
| [`backend/README.md`](backend/README.md) | Cómo levantar la API, configuración, endpoints y decisiones del backend. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Ramas, commits y pull requests. |

## Equipo

| Integrante | Rol principal |
|---|---|
| Franco Bonorino | Arquitectura y persistencia |
| Mercedes Rodriguez | UI / UX |
| Nicolás Rodriguez | Capacidades del dispositivo y testing |
| Nahuel Sanguineti | Sincronización y backend |

Responsabilidades detalladas en [`docs/preentrega.md`](docs/preentrega.md), punto 17.

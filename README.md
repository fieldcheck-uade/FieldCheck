# FieldCheck

App Android para hacer **inspecciones de campo guiadas por checklist**: por cada ítem se registra un resultado (Cumple / No cumple / N/A), una nota y fotos como evidencia. Funciona **100 % sin conexión** y sincroniza sola con un servidor cuando vuelve la señal.

Trabajo Práctico Obligatorio de **Desarrollo de Aplicaciones I** (UADE).

> **Estado actual:** Etapa 1, preentrega de análisis y diseño (entrega 09/10/2026). Toda la documentación está en [`docs/preentrega.md`](docs/preentrega.md).
>
> La implementación comienza en la Etapa 2, una vez aprobada la preentrega. Un prototipo exploratorio que se hizo para validar la arquitectura propuesta se conserva en la rama [`prototipo/arquitectura`](https://github.com/fieldcheck-uade/FieldCheck/tree/prototipo/arquitectura).
>
> La app ya tiene implementadas las cinco pantallas del Figma (P1 a P5) con MVVM + Clean Architecture + Hilt. Falta la sincronización con el backend.

---

## Qué problema resuelve

Quien hace inspecciones en obra, planta o salas técnicas trabaja en lugares con mala señal. Hoy junta fotos en la galería, notas en papel o WhatsApp, y después reconstruye todo a mano en un Excel. FieldCheck permite registrar la inspección una sola vez, en el lugar, con la evidencia asociada a cada ítem.

Detalle completo en [`docs/preentrega.md`](docs/preentrega.md).

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/preentrega.md`](docs/preentrega.md) | Los 17 entregables de la preentrega: problema, usuario, requisitos, offline first, tecnologías, roles. |
| [`docs/diagramas.md`](docs/diagramas.md) | Flujo de pantallas, arquitectura (datos e infraestructura), secuencia de sincronización y modelo de datos. |
| [Figma](https://www.figma.com/design/huvTo8nDgobJskL3ysZDSj/FieldCheck-%E2%80%94-Mockups--DevApps1-) | Wireframes y mockups de alta fidelidad con los estados de cada pantalla (exportados en [`docs/diseno/`](docs/diseno/)). |
| [`docs/pantallas.md`](docs/pantallas.md) | Especificación de pantallas y estados para Figma, y criterios de accesibilidad. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Ramas, commits y pull requests. |

## Instrucciones de ejecución

**App Android**

1. Abrir la carpeta del repo en Android Studio (necesita JDK 17 o superior y el SDK de Android 37).
2. Esperar la sincronización de Gradle y ejecutar la configuración `app` en un emulador o teléfono con Android 8.0 (API 26) o superior.
3. La primera vez se cargan cuatro plantillas de ejemplo, para poder usar la app aunque nunca haya tenido conexión. Apenas haya red y el backend esté levantado, se reemplazan por las del servidor.

Desde la terminal:

```bash
./gradlew assembleDebug        # compila la app
./gradlew testDebugUnitTest    # corre los tests unitarios (sin emulador)
```

**Backend**

Desde `backend/`: `docker compose up --build`. La API queda en `http://localhost:8000` (Swagger en `/docs`) y desde el emulador se accede en `http://10.0.2.2:8000`. Instrucciones completas, configuración y endpoints en [`backend/README.md`](backend/README.md).

**Conectar la app con el backend**

Por defecto, la app de debug apunta a `http://10.0.2.2:8000/` (la computadora vista desde el emulador) con la API key de desarrollo. Para cambiarlo, agregar en `local.properties` (no se sube al repo):

```properties
fieldcheck.apiBaseUrl=http://10.0.2.2:8000/
fieldcheck.apiKey=dev-api-key
```

En debug, la app solo permite HTTP sin cifrar hacia `10.0.2.2` y `localhost` (`app/src/debug/res/xml/network_security_config.xml`). Para probar en un teléfono físico hay que agregar ahí la IP de la computadora.

## Arquitectura

**MVVM + Clean Architecture**, con inyección de dependencias con **Hilt** y UI en **Jetpack Compose**. Toma como referencia la arquitectura de [PokedexAcademy-MVVM-Clean](https://github.com/ExBattou/PokedexAcademy-MVVM-Clean), vista en clase.

```
Pantalla (Compose) → ViewModel → Caso de uso → Repositorio (interfaz) → Room / Retrofit
     ↑ UiState (StateFlow)          ↑ Resource (Success / Error)
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
5. `HistoryScreen` dibuja cada estado. Los mensajes de una sola vez (snackbars) viajan aparte, por un `SharedFlow`, para que no se repitan al rotar la pantalla.

**Estructura de paquetes**

```
app/src/main/java/ar/edu/uade/fieldcheck/
├── FieldCheckApplication.kt   @HiltAndroidApp: arranca Hilt
├── MainActivity.kt            @AndroidEntryPoint: única Activity, contiene la UI Compose
├── core/                      lo compartido por todas las capas
│   ├── model/                 Resource (resultado de Data) y UiState (estado de pantalla)
│   ├── navigation/            rutas tipadas (Screen)
│   └── theme/                 colores, tipografía y formas (docs/pantallas.md)
├── domain/                    Kotlin puro, sin Android
│   ├── model/                 Inspection, InspectionHistory
│   ├── repository/            interfaces (contratos) de repositorio
│   └── usecase/               casos de uso con reglas de negocio
├── data/
│   ├── local/                 Room: database (con migraciones), dao, entity; archivos de fotos
│   ├── remote/                Retrofit: api, dto y FieldCheckRemoteDataSource (clasifica errores)
│   ├── mapper/                Entity ↔ Dominio y Entity ↔ DTO
│   ├── repository/            implementaciones de los repositorios
│   └── sync/                  SyncWorker y WorkManagerSyncScheduler (WorkManager)
├── di/                        módulos de Hilt (Database, DataStore, Network, Repository, Time)
└── presentation/
    ├── FieldCheckApp.kt       NavHost con el grafo de navegación
    ├── ThemeViewModel.kt      modo claro / oscuro de toda la app (guardado en DataStore)
    ├── common/components/     componentes compartidos (SyncChip, botón de modo oscuro, estados vacío/error/carga)
    ├── history/               P1 · Historial
    ├── newinspection/         P2 · Nueva inspección
    ├── execution/             P3 · Ejecución de la inspección (cámara incluida)
    ├── review/                P4 · Revisión y finalización
    └── detail/                P5 · Detalle y compartir resumen
app/src/test/                  tests unitarios con repositorios falsos y MockWebServer (sin emulador)
backend/                       API FastAPI + SQLite (ver backend/README.md)
docs/                          documentación de la preentrega
    diseno/                    wireframes, mockups y estados de pantalla
```

**Cómo se sincroniza una inspección (RF03)**

1. Al finalizar (o al tocar "Reintentar" en el detalle), el caso de uso deja la inspección en `PENDING` y pide una sincronización a `SyncScheduler`.
2. `WorkManagerSyncScheduler` encola `SyncWorker` con la condición "hay red". Sin conexión, WorkManager espera, aunque la app esté cerrada.
3. Con red, `SyncWorker` actualiza las plantillas y ejecuta `SyncPendingInspectionsUseCase`, que sube cada inspección pendiente con `SyncRepository`: primero los datos y después las fotos que falten.
4. Según el resultado:
   - Si todo sube, la inspección pasa a "Sincronizada".
   - Ante un error transitorio (sin red, timeout o 5xx), sigue "Pendiente" y WorkManager reintenta con espera exponencial.
   - Ante un error permanente (4xx, por ejemplo una API key inválida), pasa a "Error" con el motivo y espera a que el usuario toque "Reintentar".
5. La UI no se entera de la red: lee el estado desde Room, así que el chip y la tarjeta del detalle se actualizan solos.

**Cómo agregar una pantalla nueva**

1. Si hace falta, el modelo en `domain/model` y el método en la interfaz del repositorio.
2. Un caso de uso en `domain/usecase` con la regla de negocio.
3. La implementación en `data/repository` (Hilt ya la conoce por `RepositoryModule`).
4. Un `XxxViewModel` con `@HiltViewModel` que exponga `StateFlow<UiState<...>>`.
5. Un `XxxRoute` (se conecta al ViewModel) y un `XxxScreen` (solo dibuja), y la ruta en `Screen` + `FieldCheckApp`.

Estrategia **offline first**: Room es la única fuente de verdad de la UI; las inspecciones se guardan localmente y WorkManager las sincroniza al haber red, de forma idempotente por `clientUuid`. Explicación completa en `docs/preentrega.md` (punto 14) y `docs/diagramas.md`.

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje y UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09), Material 3 |
| Navegación y estado | Navigation Compose (rutas tipadas), ViewModel, StateFlow, Coroutines |
| Inyección de dependencias | Hilt (Dagger) |
| Persistencia | Room 2.8 (KSP); DataStore Preferences para las preferencias del usuario (modo oscuro) |
| Red | Retrofit 3, OkHttp 5 (log de red solo en debug), kotlinx.serialization |
| Segundo plano | WorkManager, con los workers creados por Hilt (`hilt-work`) |
| Dispositivo | Cámara del sistema (`TakePicture` + `FileProvider`), ubicación (Fused Location, opcional), hoja de compartir |
| Imágenes | Coil 3 |
| Tests | JUnit4, Turbine, kotlinx-coroutines-test, MockWebServer; repositorios falsos escritos a mano en lugar de MockK |
| Backend | FastAPI, SQLite, Docker Compose |

Justificación de cada una en `docs/preentrega.md`, punto 15.

## Funcionalidades y requisitos funcionales

| Requisito | Descripción | Estado |
|---|---|---|
| RF01 | Crear una inspección a partir de una plantilla (plantillas cacheadas) | Listo (se descargan de la API y quedan en caché) |
| RF02 | Completar ítems con resultado, nota y foto, sin conexión | Listo |
| RF03 | Sincronización automática con estado visible y reintento | Listo |
| RF04 | Historial, detalle y compartir resumen | Listo |

## Decisiones relevantes

- **Room como única fuente de verdad:** la UI nunca lee de la red, así que funciona igual con o sin conexión.
- **Sincronización al finalizar:** una inspección finalizada es de solo lectura, lo que casi elimina los conflictos; ante un conflicto, el servidor aplica *last-write-wins*.
- **Idempotencia por `clientUuid`:** reintentar un envío nunca duplica datos en el servidor.
- **Errores transitorios y permanentes:** solo se reintenta automáticamente lo que puede andar más tarde (sin red, timeout, 5xx). Un 4xx no se arregla reintentando, así que se muestra con su motivo y queda en manos del usuario.
- **Fotos marcadas de a una:** cada foto subida se marca como sincronizada, así un corte a mitad de la subida no obliga a mandar todas otra vez.
- **API key fuera del código:** se lee de `local.properties` en tiempo de compilación (`BuildConfig`) y el log de red la oculta.
- **Migración de Room 1 → 2:** se agregó `templateVersion` a las inspecciones (la pide la API) con una migración, en lugar de borrar la base, para no perder inspecciones guardadas.
- **Cámara del sistema en lugar de CameraX:** menos código y sin pedir el permiso `CAMERA`.
- **Inyección de dependencias con Hilt:** reemplaza la inyección manual planteada en la preentrega, para alinearse con la arquitectura vista en clase. Hilt crea cada dependencia una sola vez y la entrega a ViewModels y repositorios, sin armar a mano un contenedor.
- **Casos de uso entre ViewModel y repositorio:** los ViewModels hablan solo con casos de uso, como en la arquitectura de referencia. Cada caso de uso tiene una regla concreta (por ejemplo, separar y ordenar el historial), así que no son capas vacías.

## Limitaciones conocidas

- Sin login ni multiusuario: la app se autentica ante la API con una API key fija.
- Las plantillas se administran en el servidor, no se editan desde la app.
- No hay exportación a PDF: el resumen se comparte como texto, sin fotos adjuntas.
- Una inspección finalizada no se puede editar.

## Equipo

| Integrante | Rol principal |
|---|---|
| Franco Bonorino | Arquitectura y persistencia |
| Mercedes Rodriguez | UI / UX |
| Nicolás Rodriguez | Capacidades del dispositivo y testing |
| Nahuel Sanguineti | Sincronización y backend |

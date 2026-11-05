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
3. La primera vez se cargan cuatro plantillas locales (simulan la descarga desde la API, que todavía no existe). Con ellas se puede crear una inspección desde "Nueva inspección".

Desde la terminal:

```bash
./gradlew assembleDebug        # compila la app
./gradlew testDebugUnitTest    # corre los tests unitarios (sin emulador)
```

El backend se va a levantar con `docker compose up` desde `backend/` (_se completa en la Etapa 2_).

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
| **Data** | Implementa los repositorios con Room (y más adelante Retrofit). Convierte entidades a modelos con mappers. | No tiene reglas de negocio. |
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
│   ├── local/                 Room: database, dao, entity
│   ├── mapper/                Entity → Dominio
│   └── repository/            implementaciones de los repositorios
├── di/                        módulos de Hilt (DatabaseModule, RepositoryModule)
└── presentation/
    ├── FieldCheckApp.kt       NavHost con el grafo de navegación
    ├── common/components/     componentes compartidos (SyncChip, estados vacío/error/carga)
    ├── history/               P1 · Historial
    ├── newinspection/         P2 · Nueva inspección
    ├── execution/             P3 · Ejecución de la inspección (cámara incluida)
    ├── review/                P4 · Revisión y finalización
    └── detail/                P5 · Detalle y compartir resumen
app/src/test/                  tests unitarios con repositorio falso (sin emulador)
backend/                       API FastAPI (etapa de implementación)
docs/                          documentación de la preentrega
    diseno/                    wireframes, mockups y estados de pantalla
```

La sincronización con el backend (`data/remote`, `data/sync`) y la ubicación de las fotos se suman en las próximas entregas siguiendo el mismo patrón.

**Cómo agregar una pantalla nueva**

1. Si hace falta, el modelo en `domain/model` y el método en la interfaz del repositorio.
2. Un caso de uso en `domain/usecase` con la regla de negocio.
3. La implementación en `data/repository` (Hilt ya la conoce por `RepositoryModule`).
4. Un `XxxViewModel` con `@HiltViewModel` que exponga `StateFlow<UiState<...>>`.
5. Un `XxxRoute` (se conecta al ViewModel) y un `XxxScreen` (solo dibuja), y la ruta en `Screen` + `FieldCheckApp`.

Estrategia **offline first**: Room es la única fuente de verdad de la UI; las inspecciones se guardan localmente y WorkManager las sincroniza al haber red, de forma idempotente por `clientUuid`. Explicación completa en `docs/preentrega.md` (punto 14) y `docs/diagramas.md`.

## Tecnologías previstas

| Área | Tecnología |
|---|---|
| Lenguaje y UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09), Material 3 |
| Navegación y estado | Navigation Compose (rutas tipadas), ViewModel, StateFlow, Coroutines |
| Inyección de dependencias | Hilt (Dagger) |
| Persistencia | Room 2.8 (KSP) |
| Red | Retrofit 3, OkHttp 5, kotlinx.serialization |
| Segundo plano | WorkManager |
| Dispositivo | Cámara del sistema (`TakePicture` + `FileProvider`), ubicación (Fused Location, opcional), hoja de compartir |
| Imágenes | Coil 3 |
| Tests | JUnit4, MockK, Turbine, kotlinx-coroutines-test, MockWebServer, Room in-memory |
| Backend | FastAPI, SQLite, Docker Compose |

Justificación de cada una en `docs/preentrega.md`, punto 15.

## Funcionalidades y requisitos funcionales

| Requisito | Descripción | Estado |
|---|---|---|
| RF01 | Crear una inspección a partir de una plantilla (plantillas cacheadas) | Listo (plantillas locales hasta tener API) |
| RF02 | Completar ítems con resultado, nota y foto, sin conexión | Listo |
| RF03 | Sincronización automática con estado visible y reintento | Pendiente |
| RF04 | Historial, detalle y compartir resumen | Listo |

## Decisiones relevantes

- **Room como única fuente de verdad:** la UI nunca lee de la red, así que funciona igual con o sin conexión.
- **Sincronización al finalizar:** una inspección finalizada es de solo lectura, lo que casi elimina los conflictos; ante un conflicto, el servidor aplica *last-write-wins*.
- **Idempotencia por `clientUuid`:** reintentar un envío nunca duplica datos en el servidor.
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

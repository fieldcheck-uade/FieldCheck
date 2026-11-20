# FieldCheck — Preentrega de análisis y diseño

**Materia:** Desarrollo de Aplicaciones I — UADE (Facultad de Ingeniería y Ciencias Exactas)
**Instancia:** Etapa 1 — Preentrega de análisis y diseño
**Equipo:** Franco Bonorino, Mercedes Rodriguez, Nicolás Rodriguez, Nahuel Sanguineti
**Fecha de entrega:** 09/10/2026
**Repositorio:** https://github.com/fieldcheck-uade/FieldCheck
**Figma:** [FieldCheck — Mockups](https://www.figma.com/design/huvTo8nDgobJskL3ysZDSj/FieldCheck-%E2%80%94-Mockups--DevApps1-)

> **Nota (entrega final):** este documento es la preentrega tal como se aprobó. Los cambios hechos durante la implementación (Hilt en lugar de `AppContainer`, DataStore para el modo oscuro, un único trabajo de sincronización, fakes en lugar de MockK, geoetiquetado no implementado) están justificados en el [README, sección "Cambios respecto de la preentrega"](../README.md#cambios-respecto-de-la-preentrega).

> Estructura: este documento sigue uno por uno los 17 entregables mínimos de la sección 5 de la consigna. La persistencia (punto 4.12 de la consigna, que no figura como entregable separado) se desarrolla dentro del punto 14.

---

## 1. Nombre provisorio

**FieldCheck** — inspecciones de campo guiadas por checklist, con evidencia y sin depender de la señal.

---

## 2. Descripción del problema

**¿Qué problema existe?**
Las personas que hacen inspecciones en campo (por ejemplo, controles de seguridad e higiene en una obra) necesitan dejar constancia de qué revisaron, qué encontraron y con qué evidencia. Hoy esa información queda repartida entre varias herramientas que no se hablan entre sí, y armar el informe final implica reconstruir todo a mano.

**¿Quién lo experimenta?**
El inspector o auditor que recorre el lugar. De forma indirecta, también quien recibe el informe (supervisor, responsable del área), que depende de que la información llegue completa y a tiempo.

**¿En qué contexto ocurre?**
En el lugar a inspeccionar: obras en construcción, subsuelos, plantas industriales, salas técnicas. Son lugares donde la señal móvil suele ser mala o nula, el inspector está de pie, se mueve, muchas veces tiene una mano ocupada o usa guantes, y la recorrida dura entre 20 y 90 minutos.

**¿Cómo se resuelve actualmente?**
Con una combinación de: planilla impresa o anotaciones en papel, fotos sacadas con la cámara del celular (que quedan mezcladas con fotos personales en la galería), mensajes o notas de voz por WhatsApp para no olvidarse de algo, y al final una planilla de Excel que se completa en la oficina transcribiendo todo.

**¿Qué dificultades presenta la solución actual?**
- **Evidencia dispersa:** una foto no queda asociada al punto del checklist al que corresponde; hay que recordar después "esta foto era del matafuego del segundo piso".
- **Sin trazabilidad:** no queda registro confiable de cuándo y dónde se tomó cada evidencia.
- **Doble trabajo:** la inspección se hace dos veces, una en el lugar y otra al transcribir.
- **Errores de transcripción** y ítems que se olvidan de revisar porque el checklist en papel no "obliga" a completar.
- **Dependencia de la conectividad:** las herramientas online (formularios web, planillas compartidas) fallan justamente donde se trabaja.

**¿Por qué una aplicación móvil podría mejorarlo?**
Porque el celular ya está en la mano del inspector en el lugar de la inspección: tiene cámara para capturar la evidencia en el momento, puede guardar todo localmente sin señal y sincronizar cuando vuelve la conexión, y puede asociar cada foto al ítem que se está revisando. La información se registra una sola vez, en el lugar donde se genera.

---

## 3. Usuario principal

**Perfil principal: inspector/a de campo** (anclado, para el diseño y la demo, en un inspector de seguridad e higiene en obra; el mismo perfil aplica a mantenimiento de planta o revisión de salas técnicas, ya que la app trabaja con plantillas genéricas).

| Aspecto | Descripción |
|---|---|
| Características relevantes | Adulto, usa el celular a diario, pero no es "usuario técnico". Conoce bien su checklist y su trabajo; la app no le tiene que enseñar a inspeccionar, solo acompañarlo. Hace varias inspecciones por semana en lugares distintos. |
| Necesidad principal | Registrar, ítem por ítem, qué revisó, el resultado y la evidencia, sin perder nada y sin tener que transcribirlo después. |
| Contexto de uso | Recorriendo el lugar, de pie y en movimiento, con el celular en una mano; interrumpido seguido (le hablan, tiene que subir una escalera, guarda el teléfono en el bolsillo). |
| Frecuencia estimada de uso | Entre 1 y 3 inspecciones por día laboral, sesiones de 20 a 90 minutos cada una. |
| Posibles restricciones | Uso a una mano, guantes o manos sucias, poco tiempo, poca tolerancia a pasos innecesarios, batería que tiene que durar toda la jornada. |
| Condiciones de conectividad | Intermitente o nula dentro del lugar (subsuelos, estructuras de hormigón, salas cerradas); conexión normal al salir o en la oficina. |
| Condiciones ambientales | Luz variable (sol directo en exteriores, poca luz en interiores), ruido, polvo, atención dividida entre el entorno y la pantalla. |
| Dificultades actuales | Las descriptas en el punto 2: evidencia dispersa, doble carga, errores al transcribir, herramientas que dependen de la conexión. |

**Usuario secundario (fuera del alcance de la app en v1):** el supervisor que recibe el resultado. En la v1 no tiene interfaz propia; recibe el resumen compartido por el inspector (RF04) y los datos quedan en el servidor para un uso futuro.

---

## 4. Contexto de uso

**Escenario principal.** Un inspector llega a una obra para hacer el control semanal de seguridad e higiene. Antes de salir, con Wi-Fi, abrió la app y las plantillas se actualizaron solas. En la obra, en el subsuelo no hay señal. Crea una inspección con la plantilla "Control semanal de obra", escribe el nombre del sitio y empieza a recorrer: por cada ítem marca Cumple / No cumple / N/A con un toque; cuando algo no cumple saca una foto y deja una nota corta. En el medio lo interrumpen, guarda el celular y a los 10 minutos sigue exactamente donde estaba. Al terminar, finaliza la inspección. Todavía sin señal, la ve en el historial como "Pendiente de sincronizar". Cuando sale de la obra y el teléfono recupera conexión, la inspección se sube sola y pasa a "Sincronizada". Desde el detalle comparte un resumen por mail o WhatsApp al supervisor.

**Mirada de computación ubicua (Mark Weiser).** La idea de Weiser es que la tecnología bien diseñada "desaparece" en la actividad: el usuario piensa en su tarea, no en la herramienta. Aplicado a FieldCheck:

| Principio | Cómo se traduce en el diseño |
|---|---|
| La tarea es inspeccionar, no usar la app | No hay botón "Guardar": cada cambio se persiste al instante. Si el usuario se va de la pantalla, no pierde nada. |
| La conectividad no es asunto del usuario | No hay un paso obligatorio de "Sincronizar". La app detecta la conexión y sube sola; el usuario solo ve un indicador periférico del estado. |
| Información en la periferia, no en el centro | El estado offline se comunica con un banner discreto, no con diálogos que interrumpen. El progreso ("12 de 20") está siempre visible sin tener que buscarlo. |
| Reducir carga cognitiva | Un ítem por vez con tres acciones grandes; la foto se asocia sola al ítem que se está viendo; fecha y hora se registran automáticamente. |
| Acompañar el contexto, no forzarlo | Uso a una mano (acciones principales en la parte inferior de la pantalla), contraste alto para exteriores, retomar la inspección en el punto exacto donde quedó. |

---

## 5. Propuesta de solución

| Elemento | Definición |
|---|---|
| Nombre provisorio | FieldCheck |
| Descripción breve | App Android para realizar inspecciones guiadas por un checklist, registrando por cada ítem un estado, una nota y evidencia fotográfica, que funciona completamente sin conexión y sincroniza sola con un servidor cuando vuelve la señal. |
| Usuario principal | Inspector/a de campo (ver punto 3). |
| Propuesta de valor | Registrar la inspección una sola vez, en el lugar, con la evidencia asociada a cada ítem, sin depender de la señal. |
| Escenario principal de uso | Crear una inspección desde una plantilla → completar los ítems con evidencia sin conexión → finalizar → sincronización automática → compartir el resumen (ver punto 4). |
| Beneficio esperado | Se elimina la transcripción posterior, la evidencia queda trazable (qué ítem, cuándo, dónde) y el informe está disponible apenas se recupera la conexión. |

---

## 6. Justificación: ¿por qué móvil y no web o escritorio?

1. **Funcionamiento offline real en el lugar de trabajo.** Una web depende de la conexión justamente donde no la hay. Una app nativa con base de datos local (Room) permite trabajar la inspección completa sin señal.
2. **Cámara integrada al flujo.** La evidencia se captura en el momento y queda asociada al ítem, sin pasar por la galería ni por otra app.
3. **Sincronización en segundo plano.** Android permite programar la subida para cuando haya red (WorkManager), aunque la app esté cerrada. Una web no puede garantizar eso.
4. **El dispositivo ya está en la mano del usuario.** Una notebook no es viable recorriendo una obra; el celular sí, y se usa a una mano.
5. **Integración con el ecosistema del teléfono.** El resumen se comparte con la hoja de compartir de Android (mail, WhatsApp, etc.) sin integrar cada servicio.
6. **Ubicación (deseable).** El GPS permite geoetiquetar la evidencia sin que el usuario tenga que escribir dónde estaba.

---

## 7. Caso de negocio y propuesta de valor

**Tipo de valor principal:** operativo (con impacto organizacional por la trazabilidad).

**Problema → Usuario → Solución → Valor**

| Problema | Usuario | Solución | Valor generado |
|---|---|---|---|
| La evidencia de las inspecciones queda dispersa y hay que reconstruirla a mano, en lugares donde no hay señal. | Inspector/a de campo que trabaja de pie, a una mano y con conectividad intermitente. | App que guía la inspección por checklist, asocia foto y nota a cada ítem, funciona offline y sincroniza sola. | Menos retrabajo (no se transcribe), evidencia trazable por ítem, fecha y lugar, y el informe disponible apenas vuelve la conexión. |

**Cómo se podría medir el valor** (indicadores propuestos para validar con usuarios; no son resultados medidos):
- Tiempo entre el fin de la inspección y el informe disponible para el supervisor.
- Cantidad de ítems "No cumple" sin evidencia asociada (la app lo lleva a cero por regla de negocio, ver RF02).
- Minutos dedicados a transcribir después de la inspección (objetivo: cero).

---

## 8. Alcance y fuera de alcance

**Incluido en la v1**
- Descarga y caché local de plantillas de checklist desde el servidor.
- Creación de una inspección a partir de una plantilla.
- Registro por ítem de estado (Cumple / No cumple / N/A), nota opcional y foto opcional, sin conexión.
- Finalización de la inspección con validación de reglas mínimas.
- Sincronización automática con el servidor al haber conexión, con estado visible y reintento.
- Historial y detalle de inspecciones, disponibles sin conexión.
- Compartir un resumen en texto mediante la hoja de compartir de Android.
- Deseable, solo si el tiempo alcanza: geoetiquetado de la evidencia con la ubicación del dispositivo.

**Fuera de alcance (explícito)**

| Excluido | Motivo |
|---|---|
| Login y multiusuario | No aporta al flujo principal; se usa una API key fija por instalación. |
| Editor de plantillas en la app | Las plantillas se administran en el servidor; editarlas en campo no es parte del problema. |
| Exportación a PDF | El resumen en texto cubre la necesidad de compartir; el PDF suma complejidad sin cambiar el flujo. |
| Firmas digitales | Requieren un marco legal y técnico que excede la materia. |
| Notificaciones push | El estado de sincronización se ve en la app; no hay eventos externos que notificar. |
| iOS | La materia es Android. |
| Resolución avanzada de conflictos | Una inspección tiene un solo dueño; se usa *last-write-wins* (ver punto 14). |
| Interfaz para el supervisor | Se cubre compartiendo el resumen; una web de administración sería otro proyecto. |

---

## 9. Requisitos funcionales

**RF01 — Crear una inspección a partir de una plantilla**
- **Usuario:** inspector/a.
- **Descripción:** el usuario elige una plantilla de checklist de la lista disponible (descargada del servidor y guardada localmente), ingresa el nombre del sitio y crea la inspección, que se inicializa con todos los ítems de la plantilla sin completar.
- **Criterio de aceptación:** con plantillas previamente descargadas y el dispositivo en modo avión, al confirmar la creación la inspección aparece en el historial con estado "En curso", con la misma cantidad de ítems que la plantilla, y sigue disponible después de cerrar y volver a abrir la app. Si no hay plantillas guardadas y no hay conexión, la pantalla muestra un mensaje que lo explica y la acción "Reintentar".

**RF02 — Completar los ítems de la inspección sin conexión**
- **Usuario:** inspector/a.
- **Descripción:** para cada ítem, el usuario registra un estado (Cumple / No cumple / N/A), una nota opcional y una foto opcional tomada con la cámara. Cada cambio se guarda al instante. Para finalizar, todos los ítems deben tener estado y todo ítem "No cumple" debe tener al menos una nota o una foto.
- **Criterio de aceptación:** en modo avión, el usuario marca estados, escribe notas y toma fotos; si cierra la app a la fuerza y la vuelve a abrir, todos los valores y fotos registrados siguen asociados a su ítem. El indicador de progreso ("n de N") refleja los ítems completados. Si intenta finalizar con ítems sin estado o con un "No cumple" sin evidencia, la app no finaliza e indica qué ítems faltan.

**RF03 — Sincronizar automáticamente al recuperar la conexión**
- **Usuario:** inspector/a (el sistema actúa en segundo plano).
- **Descripción:** las inspecciones finalizadas se envían al servidor sin intervención del usuario cuando hay conexión: primero los datos de la inspección y luego las fotos. Cada inspección muestra su estado de sincronización (Pendiente / Sincronizada / Error) y, en caso de error, ofrece "Reintentar".
- **Criterio de aceptación:** una inspección finalizada en modo avión se muestra como "Pendiente"; al desactivar el modo avión, sin que el usuario toque nada, pasa a "Sincronizada" y en el servidor existe exactamente una inspección con ese identificador y todas sus fotos. Si el servidor rechaza el envío, la inspección queda en "Error" con un motivo legible; reintentar varias veces no genera duplicados en el servidor.

**RF04 — Consultar el historial y el detalle, y compartir un resumen**
- **Usuario:** inspector/a.
- **Descripción:** el usuario ve la lista de sus inspecciones (sitio, plantilla, fecha, estado de sincronización), entra al detalle de cualquiera (ítems, estados, notas y fotos) y puede compartir un resumen en texto con cualquier app instalada.
- **Criterio de aceptación:** en modo avión, el historial muestra todas las inspecciones guardadas en el dispositivo y el detalle muestra sus ítems, notas y miniaturas de fotos. Al tocar "Compartir" se abre la hoja de compartir de Android con un texto que incluye sitio, fecha, plantilla, cantidad de ítems por estado y la lista de ítems "No cumple" con su nota.

---

## 10. Requisitos no funcionales

Cada requisito indica cómo se verifica.

| ID | Categoría | Requisito | Verificación |
|---|---|---|---|
| RNF01 | Comportamiento offline | RF01 (con plantillas ya descargadas), RF02 y RF04 se pueden completar de punta a punta con el dispositivo en modo avión, sin errores ni pantallas bloqueadas. | Prueba manual en modo avión sobre los tres flujos. |
| RNF02 | Confiabilidad | Ningún dato confirmado por el usuario (estado, nota, foto) se pierde si la app se cierra, el sistema la mata o el teléfono se reinicia. | Forzar detención desde Ajustes a mitad de una inspección y verificar al reabrir. |
| RNF03 | Confiabilidad / sincronización | El envío es idempotente: una misma inspección enviada más de una vez queda registrada una sola vez en el servidor. Los errores transitorios (sin red, timeout, error 5xx) se reintentan solos con espera exponencial; los errores permanentes (4xx) pasan a "Error" sin reintento automático. | Test de repositorio con MockWebServer + prueba manual forzando dos envíos. |
| RNF04 | Recuperación ante errores | Todo error visible al usuario incluye qué pasó y qué puede hacer (por ejemplo, "Reintentar"). Ningún error de red provoca un cierre inesperado de la app. | Prueba manual con el servidor apagado y con API key inválida. |
| RNF05 | Usabilidad (una mano) | Marcar el estado de un ítem requiere un solo toque; las acciones principales de cada pantalla están en la mitad inferior. Al volver a una inspección en curso se abre en el primer ítem sin completar. | Revisión de diseño en Figma y prueba manual. |
| RNF06 | Accesibilidad | Áreas táctiles de al menos 48 × 48 dp; contraste de texto de al menos 4.5:1; el estado de un ítem no se comunica solo con color (siempre ícono + texto); todos los íconos accionables tienen descripción para TalkBack; con tamaño de fuente al 200 % las acciones principales siguen visibles. | Accessibility Scanner sin advertencias en las pantallas principales + prueba con TalkBack del flujo RF02. |
| RNF07 | Rendimiento / red | Las fotos se guardan redimensionadas (lado mayor 1600 px, JPEG calidad 80) para que cada una pese aproximadamente 1 MB o menos y la subida sea viable con señal débil. | Inspeccionar el tamaño de los archivos generados. |
| RNF08 | Privacidad y seguridad | Fotos y datos se guardan solo en el almacenamiento interno de la app (no aparecen en la galería ni son accesibles a otras apps); la app no pide permisos de almacenamiento; los permisos se solicitan solo en el momento en que se usan; la API key no está escrita en el código fuente; el log de red solo existe en compilaciones de debug; los datos de la app se excluyen del backup automático. | Revisión del manifest y del código + verificar la galería del dispositivo después de sacar fotos. |
| RNF09 | Batería / uso de sensores | La ubicación, si se implementa, se lee una única vez por foto y solo con la app en primer plano (sin seguimiento continuo ni ubicación en segundo plano). La sincronización solo se ejecuta cuando hay red. | Revisión de permisos declarados (sin `ACCESS_BACKGROUND_LOCATION`) y de las restricciones del trabajo de WorkManager. |
| RNF10 | Compatibilidad | Funciona en Android 8.0 (API 26) en adelante, en orientación vertical. | Ejecución del flujo principal en emuladores API 26 y en la última API estable. |
| RNF11 | Mantenibilidad / testabilidad | La capa de dominio no importa clases de Android; cada caso de uso y cada ViewModel tiene tests unitarios que se ejecutan con `./gradlew test`. | Revisión de imports + ejecución de la suite de tests. |

---

## 11. Diseño en Figma

**Link:** [FieldCheck — Mockups (Figma)](https://www.figma.com/design/huvTo8nDgobJskL3ysZDSj/FieldCheck-%E2%80%94-Mockups--DevApps1-)

El diseño se trabajó en dos niveles, ambos exportados a [`docs/diseno/`](diseno/):

- **Wireframes** de baja fidelidad del flujo completo: [`wireframes.pdf`](diseno/wireframes.pdf).
- **Mockups** de alta fidelidad sobre Material 3 (frame Android 360 × 800 dp), con los estados relevantes de cada pantalla y la hoja de componentes compartidos.

La especificación detallada de cada pantalla (componentes, jerarquía, acciones, estados y microcopy) está en `docs/pantallas.md`.

| # | Pantalla | Objetivo | Estados diseñados |
|---|---|---|---|
| P1 | Historial (inicio) | Ver inspecciones y su estado de sincronización; punto de entrada a crear una nueva. | Carga, contenido, vacío, offline. |
| P2 | Nueva inspección | Elegir plantilla e ingresar el sitio. | Contenido (hoja inferior), desactualizado sin conexión, vacío sin conexión. |
| P3 | Ejecución de inspección | Completar ítem por ítem con estado, nota y foto. | "No cumple" sin evidencia, offline en modo oscuro. |
| P4 | Revisión y finalización | Ver el resumen, detectar faltantes y finalizar. | Contenido completo, validación con faltantes. |
| P5 | Detalle de inspección | Consultar una inspección y compartir su resumen. | Pendiente, sincronizada, error de sincronización. |

### Componentes compartidos

<img src="diseno/componentes.png" width="560" alt="Componentes: SyncChip, ResultSelector y OfflineBanner">

### P1 — Historial

<img src="diseno/p1-historial-carga.png" width="180" alt="P1 carga"> <img src="diseno/p1-historial-contenido.png" width="180" alt="P1 contenido"> <img src="diseno/p1-historial-vacio.png" width="180" alt="P1 vacío"> <img src="diseno/p1-historial-offline.png" width="180" alt="P1 offline">

*Carga · Contenido · Vacío · Offline*

### P2 — Nueva inspección

<img src="diseno/p2-nueva-inspeccion-hoja-inferior.png" width="180" alt="P2 hoja inferior"> <img src="diseno/p2-nueva-inspeccion-desactualizada.png" width="180" alt="P2 desactualizado sin conexión"> <img src="diseno/p2-nueva-inspeccion-vacio-sin-conexion.png" width="180" alt="P2 vacío sin conexión">

*Hoja inferior para empezar · Plantillas desactualizadas sin conexión · Sin plantillas y sin conexión*

### P3 — Ejecución de la inspección

<img src="diseno/p3-ejecucion-no-cumple-sin-evidencia.png" width="180" alt="P3 no cumple sin evidencia"> <img src="diseno/p3-ejecucion-oscuro-sin-conexion.png" width="180" alt="P3 modo oscuro sin conexión">

*"No cumple" sin evidencia · Modo oscuro sin conexión*

### P4 — Revisión y finalización

<img src="diseno/p4-revision-completa.png" width="180" alt="P4 contenido completo"> <img src="diseno/p4-revision-faltantes.png" width="180" alt="P4 validación con faltantes">

*Contenido completo · Validación con faltantes*

### P5 — Detalle de inspección

<img src="diseno/p5-detalle-pendiente.png" width="180" alt="P5 pendiente"> <img src="diseno/p5-detalle-sincronizada.png" width="180" alt="P5 sincronizada"> <img src="diseno/p5-detalle-error.png" width="180" alt="P5 error de sincronización">

*Pendiente de subir · Sincronizada · Error de sincronización*

**Criterios que guían el diseño:** una sola tarea por pantalla, acciones principales al alcance del pulgar, feedback inmediato en cada acción (guardado, foto adjunta, estado de sincronización), prevención de errores antes que mensajes de error (validación al finalizar con navegación directa al ítem faltante), estados comunicados siempre con ícono + texto y no solo con color, y alto contraste para uso en exteriores.

---

## 12. Flujo de pantallas

El diagrama del flujo del caso de uso principal (punto de entrada, pantallas, decisiones, operaciones críticas y finales posibles) está en `docs/diagramas.md`, sección 1.

Resumen del recorrido: la app abre en el **Historial** → el usuario toca **Nueva inspección** → elige plantilla y sitio → **Ejecución** ítem por ítem (con cámara del sistema para la evidencia) → **Revisión** → si hay faltantes vuelve al ítem correspondiente; si no, **Finaliza** → vuelve al Historial con la inspección "Pendiente" → la sincronización ocurre en segundo plano → desde el **Detalle** puede compartir el resumen.

---

## 13. Diagrama de arquitectura

Los diagramas de flujo de datos por capas y de topología de infraestructura están en `docs/diagramas.md` (secciones 2 y 3), junto con el diagrama de secuencia de la sincronización y el modelo de datos.

**Patrón:** MVVM con principios de Clean Architecture, en un único módulo `app` organizado en paquetes por capa.

`UI (Compose) → ViewModel → Caso de uso → Repositorio (interfaz) → Fuente local (Room) / Fuente remota (Retrofit)`

| Capa | Responsabilidad | Contenido en FieldCheck |
|---|---|---|
| Presentación | Dibujar la UI, exponer el estado de pantalla y recibir eventos del usuario. | Pantallas Compose, ViewModels con `StateFlow<UiState>`, navegación. |
| Dominio | Modelos y reglas de negocio, independientes de Android. | Modelos (`Inspection`, `ItemResult`, `Evidence`, `Template`), interfaces de repositorio, casos de uso con reglas reales (por ejemplo, `FinalizeInspectionUseCase`: valida que todos los ítems tengan estado y que todo "No cumple" tenga evidencia). |
| Datos | Obtener y guardar datos; ocultar de dónde vienen. | Implementaciones de repositorio, DAOs y entidades de Room, API de Retrofit y DTOs, mappers Entity ↔ Dominio ↔ DTO, worker de sincronización. |

**Reglas de dependencia:** presentación depende de dominio; datos depende de dominio (implementa sus interfaces); dominio no depende de nadie. La UI nunca lee de la red: solo observa Room a través del repositorio.

**Casos de uso previstos:** solo donde hay una regla de negocio. `CreateInspectionUseCase` (copia los ítems de la plantilla a la inspección), `FinalizeInspectionUseCase` (validación y encolado de sincronización) y `BuildInspectionSummaryUseCase` (arma el texto a compartir). Las lecturas simples (listar historial) van directo del ViewModel al repositorio.

**Inyección de dependencias:** manual, con un `AppContainer` creado en la clase `Application`.

---

## 14. Estrategia Offline First y persistencia

### 14.1 Principio

**Room es la única fuente de verdad para la UI.** Las pantallas observan la base local (Flow); la red solo se usa para traer plantillas hacia Room y para enviar inspecciones desde Room. Así la app se comporta igual con o sin conexión, y la diferencia solo se ve en el estado de sincronización.

### 14.2 Qué se persiste (punto 4.12 de la consigna)

| Entidad | Origen | Contenido principal | Relación |
|---|---|---|---|
| `Template` | API (se cachea) | id, nombre, versión, fecha de descarga | 1 → N `TemplateItem` |
| `TemplateItem` | API (se cachea) | id, texto del ítem, orden | N → 1 `Template` |
| `Inspection` | Usuario | `clientUuid`, sitio, plantilla de origen, fechas de inicio/fin, estado (En curso / Finalizada), `syncStatus`, `updatedAt`, último error | 1 → N `ItemResult` |
| `ItemResult` | Usuario | copia del texto del ítem, orden, estado (Cumple / No cumple / N/A / sin completar), nota, `updatedAt` | N → 1 `Inspection`; 1 → N `Evidence` |
| `Evidence` | Usuario | `clientUuid`, ruta del archivo en almacenamiento interno, fecha de captura, latitud/longitud (opcionales), `syncStatus` | N → 1 `ItemResult` |

- Las **fotos** se guardan como archivos en el directorio interno de la app; en Room solo se guarda la ruta.
- **Decisión:** `ItemResult` copia el texto del ítem al crear la inspección. Si la plantilla cambia en el servidor, las inspecciones ya hechas no se alteran (una inspección es un registro histórico).
- **Cola de sincronización:** en lugar de una tabla aparte, el propio campo `syncStatus` de `Inspection` y `Evidence` funciona como cola (patrón *outbox*): el worker busca lo que está en `PENDING` o `FAILED`.

### 14.3 Qué pasa en cada situación de conectividad

| Situación | Comportamiento |
|---|---|
| **Tiene conexión** | Al abrir la app se actualizan las plantillas en segundo plano. Al finalizar una inspección se encola su sincronización y se ejecuta de inmediato. |
| **Pierde conexión** | El usuario sigue trabajando igual: todo se guarda en Room. Aparece un banner discreto "Sin conexión — tus cambios se guardan en el teléfono". Las inspecciones finalizadas quedan "Pendiente". |
| **Recupera conexión** | WorkManager (con restricción de red conectada) ejecuta el trabajo encolado: primero envía los datos de la inspección, después cada foto. Cada paso marca su registro como `SYNCED`. El banner desaparece. |
| **Tiene información local desactualizada** | Solo aplica a las plantillas. Si no se pudieron actualizar, se usan las guardadas y se muestra su antigüedad ("Plantillas actualizadas hace 3 días"). Las inspecciones no se desactualizan: el dispositivo es su origen. |
| **No tiene información local todavía** | Primera apertura sin conexión: el historial muestra su estado vacío normal; la pantalla de nueva inspección explica que hace falta conexión para descargar las plantillas por primera vez y ofrece "Reintentar". |

### 14.4 Respuestas a las preguntas de la consigna

- **Qué se almacena localmente:** plantillas (caché), inspecciones, resultados por ítem, evidencias (fotos en archivos + metadatos) y el estado de sincronización de cada registro.
- **Fuente remota:** API REST propia (FastAPI + SQLite) con tres endpoints: `GET /templates`, `POST /inspections` (idempotente por `client_uuid`) y `POST /inspections/{client_uuid}/evidence` (multipart). Autenticación con API key en un header.
- **Cuándo se actualizan los datos:** plantillas, al abrir la app con conexión; inspecciones, al finalizarlas (y cada vez que vuelve la conexión, si quedaron pendientes).
- **Qué ve el usuario sin conexión:** toda la app funciona; el historial y el detalle muestran lo guardado, se puede crear y completar inspecciones con las plantillas en caché, y cada inspección indica si está pendiente de subir.
- **Errores y conflictos:**
  - *Errores transitorios* (sin red, timeout, 5xx): el worker reintenta con espera exponencial; si supera el máximo de intentos, la inspección queda en "Error" con "Reintentar" manual.
  - *Errores permanentes* (401 por API key inválida, 422 por datos inválidos): se marca "Error" con el motivo, sin reintento automático.
  - *Duplicados:* cada inspección y cada foto tienen un `clientUuid` generado en el dispositivo; el servidor usa ese identificador para no crear duplicados si recibe el mismo envío dos veces (por ejemplo, si se cortó la conexión antes de recibir la respuesta).
  - *Conflictos:* se aplica *last-write-wins* por `updatedAt` en el servidor. Es una decisión consciente: cada inspección la crea y la edita un único inspector desde un único dispositivo, por lo que dos versiones distintas de la misma inspección son un caso excepcional. Implementar una resolución de conflictos más compleja agregaría costo sin resolver un problema real del usuario.

---

## 15. Tecnologías previstas y justificación

Para cada decisión relevante se indica qué problema resuelve, en qué capa vive y qué alternativa más simple se descartó.

| Tecnología | Qué problema resuelve | Capa | Alternativa descartada y por qué |
|---|---|---|---|
| Kotlin + Jetpack Compose + Material 3 | UI declarativa donde cada estado de pantalla (carga, vacío, error, offline) se dibuja a partir de un único objeto de estado. Material 3 trae componentes accesibles y áreas táctiles de 48 dp por defecto. | Presentación | Vistas XML: más código para representar los mismos estados y no es el enfoque de la materia. |
| Navigation Compose (rutas tipadas) | Navegar entre pantallas pasando el id de la inspección con chequeo en compilación. | Presentación | Manejar pantallas con un `when` manual: se complica con la pila de navegación y el botón atrás. |
| ViewModel + StateFlow + Coroutines/Flow | Mantener el estado de la pantalla ante rotaciones y exponerlo como una clase sellada `UiState` (Loading / Content / Empty / Error / Offline). Flow permite que la UI reaccione sola a los cambios en Room. | Presentación | `LiveData`: menos integrado con coroutines y con Compose. |
| Room (con KSP) | Persistencia estructurada con relaciones (inspección → ítems → fotos), consultas observables y única fuente de verdad offline. | Datos | Archivos JSON o DataStore: no sirven para datos relacionales ni para consultar por estado de sincronización. |
| Retrofit + OkHttp + kotlinx.serialization | Consumir la API REST (descargar plantillas, enviar inspecciones y fotos multipart) con interfaces declarativas. El log de red solo se habilita en debug. | Datos | `HttpURLConnection` a mano: mucho código repetido para multipart, headers y parseo. |
| WorkManager | Ejecutar la sincronización cuando haya red, aunque la app esté cerrada, con reintentos y espera exponencial. Un trabajo único por inspección evita envíos duplicados en paralelo. | Datos | Sincronizar desde el ViewModel: se corta si el usuario cierra la app o si no hay red en ese momento. |
| `ConnectivityManager.NetworkCallback` como `Flow<Boolean>` | Mostrar en la UI si hay conexión (banner offline). No decide la sincronización, que queda a cargo de WorkManager. | Datos → Presentación | Consultar la red solo cuando se necesita: la UI no se enteraría de los cambios en tiempo real. |
| `ActivityResultContracts.TakePicture` + `FileProvider` | Tomar la foto con la cámara del sistema y guardarla directo en el almacenamiento interno de la app, sin que pase por la galería. No requiere el permiso `CAMERA` porque la captura la hace la app de cámara. | Presentación (captura) / Datos (archivo) | CameraX: más control sobre la cámara, pero mucho más código y un permiso más que pedir, sin beneficio para el usuario. |
| Coil | Mostrar miniaturas de fotos locales de forma eficiente (caché y carga asíncrona). | Presentación | Decodificar bitmaps a mano: riesgo de problemas de memoria con fotos grandes. |
| FusedLocationProviderClient (deseable) | Geoetiquetar cada foto con una lectura puntual de ubicación. Si el permiso se rechaza, la foto se guarda igual sin ubicación y se informa. | Datos | `LocationManager`: más código y más consumo para el mismo resultado. |
| `Intent.ACTION_SEND` | Compartir el resumen con cualquier app instalada (mail, WhatsApp, Drive). | Presentación | Integrar cada servicio por separado: fuera de alcance y dependiente de terceros. |
| Inyección de dependencias manual (`AppContainer`) | Construir repositorios y casos de uso en un solo lugar y poder reemplazarlos por dobles en los tests. | Transversal | Hilt o Koin: la consigna no lo pide y agrega anotaciones y generación de código difíciles de explicar para un proyecto de este tamaño. |
| FastAPI + SQLite + Docker Compose | Fuente remota propia, controlada por el equipo, que permite implementar y probar la sincronización, la idempotencia y el manejo de errores. | Infraestructura | API pública de terceros: no permite enviar inspecciones ni probar los casos de error. Firebase: resuelve la sincronización como una caja negra y no permite controlar la idempotencia ni la estrategia de reintentos. |
| JUnit4, MockK, Turbine, kotlinx-coroutines-test, Room in-memory, MockWebServer | Verificar reglas de negocio (casos de uso), estados del ViewModel, consultas de Room y el comportamiento del repositorio remoto ante errores. | Tests | Solo pruebas manuales: no permiten verificar RNF03 ni RNF11. |

**Consideradas y no incluidas:** DataStore (la fecha de descarga de plantillas se guarda en Room junto con la plantilla, así que no aporta), CameraX y Hilt (ver tabla).

---

## 16. Repositorio

- **URL:** https://github.com/fieldcheck-uade/FieldCheck (público)
- **Organización:** `fieldcheck-uade`, con todos los integrantes como *owners* en igualdad de condiciones.
- **Integrantes con acceso:** Franco Bonorino, Mercedes Rodriguez, Nicolás Rodriguez y Nahuel Sanguineti (owners de la organización).

**Estrategia de trabajo**
- `main` siempre compila y contiene solo trabajo revisado. Está protegida con un *ruleset* de GitHub: no admite push directo ni *force push*, y todo cambio necesita un pull request con al menos una aprobación.
- Una rama por unidad de trabajo, nombrada por tipo y requisito: `feature/rf02-completar-items`, `fix/sync-duplicados`, `docs/diagramas`.
- Todo cambio entra a `main` por pull request con al menos una revisión de otro integrante.
- Los PR se integran con *merge commit* (no *squash*) para conservar la historia real de commits de cada integrante.
- `prototipo/arquitectura`: rama con un prototipo exploratorio que se hizo antes de la preentrega para validar la arquitectura propuesta (pantallas en Compose, capa de dominio y Room). No forma parte de esta entrega: `main` contiene solo la documentación de la Etapa 1, y el prototipo se retoma en la Etapa 2, ajustado según las correcciones.

**Convención de commits** (en español, formato *Conventional Commits*)
- `tipo(alcance): descripción en imperativo`, por ejemplo `feat(sync): encolar inspección al finalizar` o `test(domain): validar finalización con ítems incompletos`.
- Tipos: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`.
- Commits chicos y frecuentes: cada commit hace una sola cosa y deja el proyecto compilando.

**Convención de pull requests**
- Título con el mismo formato que los commits; descripción con qué cambia, qué requisito cubre (RF/RNF) y cómo se probó.
- PR chicos (idealmente menos de 400 líneas cambiadas); si crece, se divide.

---

## 17. Integrantes, roles y responsabilidades

| Integrante | Rol principal | Responsabilidades | Áreas en las que participa |
|---|---|---|---|
| Franco Bonorino | Arquitectura y persistencia | Estructura MVVM + Clean Architecture, capa de dominio (modelos, interfaces, casos de uso), Room (entidades, DAOs, mappers) e inyección de dependencias (`AppContainer`). | Integración entre capas, revisión de pull requests. |
| Mercedes Rodriguez | UI / UX | Diseño en Figma (pantallas, estados y componentes), sistema visual y accesibilidad (RNF05, RNF06), pantallas en Jetpack Compose. | Detalle de inspección y compartir resumen (RF04). |
| Nicolás Rodriguez | Capacidades del dispositivo y testing | Cámara con `FileProvider`, ubicación (deseable), manejo de permisos y sus estados de rechazo; tests de casos de uso y ViewModels. | Pruebas manuales en modo avión (RNF01, RNF02). |
| Nahuel Sanguineti | Sincronización y backend | API REST (FastAPI + SQLite), cliente Retrofit, sincronización con WorkManager, idempotencia y manejo de errores (RF03, RNF03, RNF04). | Topología de infraestructura, README técnico. |

**Cambios en el equipo:** ninguno a la fecha.

Los roles no son exclusivos: todos los integrantes conocen el funcionamiento general de la aplicación y participan de la revisión de pull requests.

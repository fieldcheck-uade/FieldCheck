# FieldCheck — Especificación de pantallas (base para Figma)

Esta especificación describe qué tiene que mostrar cada pantalla, cómo se ordena, qué puede hacer el usuario y cómo se ve en cada estado. Es el insumo para armar el Figma (entregable 11) y después para implementar en Compose.

**Contexto de diseño (de la preentrega, punto 3):** el inspector usa la app de pie, a una mano, a veces con guantes, con luz variable (sol directo o interiores oscuros), interrumpido y sin señal. De ahí salen las tres reglas que atraviesan todo el diseño:

1. **Lo que se hace seguido se hace con el pulgar:** las acciones principales van en la mitad inferior de la pantalla.
2. **Nada se pierde y nada requiere "guardar":** todo cambio se persiste al instante y la app lo confirma.
3. **El estado del sistema se ve siempre, sin interrumpir:** conexión y sincronización se muestran en la periferia (banner, chips), no con diálogos.

---

## 0. Guía para armar el Figma

- **Frame:** Android compacto, 360 × 800 dp, orientación vertical. Grilla de 8 dp, márgenes laterales de 16 dp.
- **Base:** kit oficial de Material 3 para Figma (Material 3 Design Kit, archivo de la comunidad), sin color dinámico: los colores de estado tienen que ser siempre los mismos.
- **Nombres de frames:** `P<n> / <Estado>`, por ejemplo `P2 / Vacío sin conexión`. Cada estado de la sección 3 es un frame.
- **Componentes con variantes:** los de la sección 2 se arman una vez como componentes con variantes (por ejemplo, `SyncChip` con variante Pendiente / Sincronizada / Error).
- **Prototipo:** conectar los frames siguiendo el diagrama 1 de `docs/diagramas.md`, para que se pueda recorrer el caso de uso principal de punta a punta.
- **Modo oscuro:** duplicar al menos P3 (la pantalla de más uso) en modo oscuro, para mostrar el uso en interiores con poca luz.

---

## 1. Fundamentos visuales

### 1.1 Colores

Todos los pares texto/fondo cumplen WCAG AA (≥ 4.5:1). Relaciones de contraste calculadas con la fórmula de WCAG 2.x.

| Token | Claro | Oscuro | Uso | Contraste |
|---|---|---|---|---|
| `background` / `surface` | `#FAFAFC` / `#FFFFFF` | `#121316` | Fondo | — |
| `onSurface` | `#1B1B1F` | `#E4E2E6` | Texto principal | 16.5:1 / 14.4:1 |
| `onSurfaceVariant` | `#44474F` | `#C4C6D0` | Texto secundario | 8.9:1 / 10.9:1 |
| `primary` / `onPrimary` | `#1F4E8C` / `#FFFFFF` | `#A8C7FA` / `#0A305F` | Botones principales, FAB | 8.3:1 / 7.6:1 |
| `complies` / `onComplies` | `#1E6B34` / `#FFFFFF` | `#7DDB93` / `#0B2913` | Estado "Cumple" | 6.5:1 / 9.3:1 |
| `notComplies` / `onNotComplies` | `#B3261E` / `#FFFFFF` | `#FFB4AB` / `#690005` | Estado "No cumple", errores | 6.5:1 / 7.7:1 |
| `notApplicable` / `onNotApplicable` | `#5C5F66` / `#FFFFFF` | `#C4C6D0` / `#1B1B1F` | Estado "N/A" | 6.4:1 / 10.1:1 |
| `offlineBanner` / `onOfflineBanner` | `#FFE08A` / `#1B1B1F` | `#FFD54F` / `#1B1B1F` | Banner sin conexión | 13.3:1 / 12.2:1 |
| `outline` | `#74777F` | `#8E9099` | Bordes de campos y tarjetas | 4.5:1 / 5.8:1 (mínimo para no-texto: 3:1) |

**Regla:** el color nunca es el único portador de significado. Todo estado lleva **ícono + texto**, además del color (ver sección 4).

### 1.2 Tipografía

Roboto (la del sistema). Tamaños en `sp`, para que respeten la configuración de tamaño de fuente del usuario.

| Estilo M3 | Tamaño | Uso |
|---|---|---|
| `headlineSmall` | 24 sp | Texto del ítem en ejecución (P3) |
| `titleLarge` | 22 sp | Títulos de pantalla |
| `titleMedium` | 16 sp, medium | Títulos de tarjetas |
| `bodyLarge` | 16 sp | Texto general, notas |
| `labelLarge` | 14 sp, medium | Botones, chips |

No se usa texto menor a 14 sp en contenido que haya que leer en campo.

### 1.3 Tamaños táctiles y espaciado

| Elemento | Tamaño mínimo | Motivo |
|---|---|---|
| Cualquier elemento tocable | 48 × 48 dp | Mínimo de Material y de accesibilidad de Android. |
| Botones de resultado (P3) | Alto 64 dp, ancho ⅓ de pantalla | Acción más frecuente; uso con guantes. |
| Botón principal de pantalla | Alto 56 dp, ancho completo | Fácil de alcanzar con el pulgar. |
| Separación entre tocables | 8 dp | Evita toques accidentales. |

---

## 2. Componentes compartidos

| Componente | Descripción | Variantes / estados |
|---|---|---|
| `OfflineBanner` | Franja bajo la barra superior: ícono de nube tachada + "Sin conexión. Tus cambios se guardan en el teléfono." No se puede cerrar, desaparece sola al volver la red. | Visible / oculto. |
| `SyncChip` | Chip chico con ícono + texto del estado de sincronización. | **Pendiente** (reloj, `onSurfaceVariant`), **Sincronizada** (tilde en nube, `complies`), **Error** (signo de admiración, `notComplies`). Una inspección "En curso" no muestra chip: todavía no se sincroniza. |
| `InspectionCard` | Tarjeta del historial: sitio (título), plantilla, fecha y hora, progreso o `SyncChip`. Toda la tarjeta es tocable. | En curso (con barra de progreso "n de N") / Finalizada (con `SyncChip`). |
| `ResultSelector` | Tres botones de igual tamaño en fila: **Cumple** (tilde), **No cumple** (cruz), **N/A** (guion). Seleccionado: fondo lleno del color del estado. No seleccionado: contorno. Se comporta como un grupo de opciones (radio). | Ninguno / uno seleccionado. |
| `EvidenceRow` | Fila horizontal de miniaturas de 72 dp con el botón "Agregar foto" al final. Cada miniatura muestra un ícono de ubicación si tiene coordenadas. | Sin fotos (solo el botón) / con fotos. |
| `EmptyState` | Ícono grande, título, texto explicativo de una línea y una acción. | Según pantalla. |
| `ErrorState` | Como `EmptyState`, con ícono de error, qué pasó en lenguaje simple y "Reintentar". | Según pantalla. |
| `Snackbar` | Confirmaciones breves ("Foto guardada") y errores recuperables, en la parte inferior. Si tiene acción, dura hasta que el usuario la toca o la descarta. | Informativo / con acción. |

---

## 3. Pantallas

### P1 — Historial (pantalla de inicio)

**Objetivo:** ver qué inspecciones hay, en qué estado están y empezar una nueva. Cubre RF04 (parte de historial) y es el punto de entrada.

**Jerarquía (de arriba hacia abajo)**
1. Barra superior: "FieldCheck".
2. `OfflineBanner` (si corresponde).
3. Sección **"En curso"** (solo si hay): `InspectionCard` con progreso. Va primero porque es lo que el inspector tiene que retomar.
4. Sección **"Finalizadas"**: `InspectionCard` ordenadas de más reciente a más antigua, con `SyncChip`.
5. FAB extendido abajo a la derecha: **"+ Nueva inspección"**.

**Acciones**
| Acción | Resultado |
|---|---|
| Tocar "Nueva inspección" | Va a P2. |
| Tocar una tarjeta "En curso" | Va a P3, en el primer ítem sin completar. |
| Tocar una tarjeta "Finalizada" | Va a P5. |

**Estados**
| Estado | Qué se ve |
|---|---|
| Carga | Tres tarjetas esqueleto (grises, sin texto). Solo aparece si la lectura local tarda; en general es imperceptible. |
| Contenido | Secciones y tarjetas como se describe arriba. |
| Vacío | `EmptyState`: portapapeles con tilde, "Todavía no hay inspecciones", "Creá la primera con el botón de abajo." El FAB sigue visible. |
| Error | `ErrorState`: "No pudimos leer las inspecciones guardadas." + "Reintentar". Caso raro (falla de la base local), pero tiene que estar contemplado. |
| Offline | El mismo contenido con `OfflineBanner` arriba. Nada se deshabilita. |
| Con error de sincronización | Contenido normal; la tarjeta afectada muestra `SyncChip` **Error**. El reintento está en P5, para no poner acciones de más en la lista. |

---

### P2 — Nueva inspección

**Objetivo:** elegir una plantilla, indicar el sitio y empezar. Cubre RF01.

**Jerarquía**
1. Barra superior: flecha atrás + "Nueva inspección".
2. `OfflineBanner` (si corresponde).
3. Línea de antigüedad de la caché: "Plantillas actualizadas hoy, 09:14" o "hace 3 días".
4. Lista de plantillas: tarjeta con nombre, cantidad de ítems ("20 ítems") y versión.
5. Al tocar una plantilla se abre una **hoja inferior** (bottom sheet) con:
   - Nombre de la plantilla elegida.
   - Campo **"Sitio"** (obligatorio, con foco y teclado abiertos automáticamente). Ejemplo: "Obra Av. Siempreviva 742 — Subsuelo 2".
   - Botón de ancho completo **"Empezar inspección"**, deshabilitado mientras el campo esté vacío.

La hoja inferior deja el campo y el botón en la zona del pulgar y evita una pantalla más.

**Acciones**
| Acción | Resultado |
|---|---|
| Tocar una plantilla | Abre la hoja inferior. |
| "Empezar inspección" | Crea la inspección y va a P3, ítem 1. P2 sale de la pila: "atrás" desde P3 vuelve a P1. |
| Deslizar la lista hacia abajo | Actualiza las plantillas (solo con conexión). |
| Atrás | Vuelve a P1. |

**Estados**
| Estado | Qué se ve |
|---|---|
| Carga (primera descarga) | Indicador circular + "Descargando plantillas…". Solo pasa sin caché y con conexión. |
| Actualizando | La lista guardada se ve normal, con una barra de progreso fina arriba. El usuario puede elegir sin esperar. |
| Contenido | Lista de plantillas + línea de antigüedad. |
| Contenido desactualizado (offline) | Lista guardada + `OfflineBanner` + "Plantillas actualizadas hace 3 días. Se van a actualizar cuando haya conexión." Se puede usar normalmente. |
| Vacío sin conexión (sin caché) | `EmptyState`: nube tachada, "Necesitás conexión la primera vez", "Las plantillas se descargan una sola vez y después funcionan sin señal." + "Reintentar". |
| Vacío (el servidor no tiene plantillas) | `EmptyState`: "No hay plantillas disponibles", "Pedile al responsable que cargue una plantilla." + "Reintentar". |
| Error (sin caché, falla el servidor) | `ErrorState`: "No pudimos descargar las plantillas. Probá de nuevo en unos minutos." + "Reintentar". |
| Error al actualizar (con caché) | Se muestra la lista guardada y un `Snackbar`: "No se pudieron actualizar las plantillas. Usás las guardadas." No bloquea. |
| Validación del campo | Si el sitio queda vacío, el botón sigue deshabilitado y el campo muestra "Ingresá el sitio" al perder el foco. |

---

### P3 — Ejecución de la inspección (pantalla principal)

**Objetivo:** completar un ítem por vez con el menor esfuerzo posible. Cubre RF02. Es la pantalla donde el inspector pasa el 90 % del tiempo: su diseño es el más importante del Figma.

**Jerarquía**
1. **Barra superior:** atrás + nombre del sitio (una línea, con puntos suspensivos si es largo) + acción **"Ver todos"** (ícono de lista, abre P4).
2. **Progreso:** "Ítem 7 de 20", con una barra lineal debajo.
3. `OfflineBanner` (si corresponde).
4. **Zona de lectura (mitad superior):**
   - Texto del ítem en `headlineSmall` (24 sp), por ejemplo "Los matafuegos están señalizados y con la carga vigente".
   - Si el ítem ya tiene resultado: una línea "Guardado ✓" que confirma que quedó registrado.
5. **Zona de evidencia:**
   - Campo **"Nota (opcional)"**, de varias líneas y expandible.
   - `EvidenceRow` con el botón **"Agregar foto"** (ícono de cámara + texto).
   - Si el resultado es "No cumple" y todavía no hay nota ni foto: un aviso en `notComplies` con ícono: "Agregá una nota o una foto como evidencia." Informa sin bloquear; el bloqueo real ocurre al finalizar.
6. **Zona del pulgar (fija abajo):**
   - `ResultSelector` (Cumple / No cumple / N/A), 64 dp de alto.
   - Fila de navegación: **"Anterior"** (texto) a la izquierda y **"Siguiente"** a la derecha. En el último ítem, "Siguiente" pasa a ser **"Revisar y finalizar"**.

**Comportamiento clave**
- **Guardado automático:** cada toque en el `ResultSelector`, cada cambio de nota (al dejar de escribir) y cada foto se guardan al instante. No existe un botón "Guardar".
- **Avance automático:** al marcar **Cumple** o **N/A**, la pantalla pasa sola al siguiente ítem después de una pausa breve (≈ 400 ms) para que se vea la selección. Al marcar **No cumple** **no** avanza, porque lo esperable es agregar evidencia. Así, la mayoría de los ítems se completa con un solo toque.
- **Vibración corta** al marcar un resultado, como confirmación sin tener que mirar la pantalla.
- **Retomar:** si se sale y se vuelve, la pantalla abre en el primer ítem sin completar.

**Acciones**
| Acción | Resultado |
|---|---|
| Tocar Cumple / N/A | Guarda y avanza al siguiente ítem. |
| Tocar No cumple | Guarda y se queda en el ítem para agregar evidencia. |
| Tocar el resultado ya seleccionado | Lo deja sin completar (permite corregir un toque accidental). |
| Escribir una nota | Se guarda al dejar de escribir. |
| "Agregar foto" | Abre la cámara del sistema. Al volver con la foto: miniatura en la fila + `Snackbar` "Foto guardada". Si se cancela, no pasa nada. |
| Mantener apretada una miniatura | Diálogo "¿Eliminar esta foto?" con **Cancelar** / **Eliminar**. |
| Tocar una miniatura | Muestra la foto a pantalla completa (diálogo con "Cerrar"). |
| "Anterior" / "Siguiente" | Navega entre ítems. |
| "Ver todos" o "Revisar y finalizar" | Va a P4. |
| Atrás | Vuelve a P1. No hace falta confirmar: no se pierde nada, y la inspección queda "En curso". |

**Estados**
| Estado | Qué se ve |
|---|---|
| Carga | Indicador circular breve mientras se lee la inspección de Room. |
| Contenido — ítem sin completar | Ningún botón del `ResultSelector` seleccionado. |
| Contenido — ítem completado | Botón seleccionado con fondo lleno + "Guardado ✓". |
| "No cumple" sin evidencia | Aviso en `notComplies` (ver jerarquía, punto 5). |
| Foto guardada | Miniatura nueva + `Snackbar` "Foto guardada". |
| Offline | Igual que el contenido + `OfflineBanner`. Todas las acciones siguen disponibles. |
| Permiso de ubicación: primer pedido | La primera vez que se toca "Agregar foto", antes del diálogo del sistema, una hoja inferior explica: "¿Querés que las fotos guarden dónde se tomaron? Sirve como respaldo de la evidencia. Podés usar la app igual si no lo permitís." + **"Continuar"**. Después aparece el diálogo del sistema y, sea cual sea la respuesta, se abre la cámara. |
| Permiso de ubicación rechazado | La foto se guarda igual. `Snackbar`: "Foto guardada sin ubicación." La miniatura no lleva el ícono de ubicación. |
| Permiso rechazado de forma permanente | Tarjeta informativa arriba de la `EvidenceRow`, que se puede cerrar: "La ubicación está desactivada para FieldCheck. Las fotos se guardan sin coordenadas." + **"Abrir ajustes"**. No se vuelve a pedir el permiso. |
| Ubicación no disponible (GPS apagado o sin fix) | La foto se guarda sin coordenadas y se informa igual que en el rechazo. La lectura de ubicación nunca demora la foto. |
| Error: no hay app de cámara | `Snackbar`: "No encontramos una app de cámara en el dispositivo." |
| Error al guardar la foto (sin espacio) | `Snackbar` con acción: "No se pudo guardar la foto. Liberá espacio e intentá de nuevo." + **"Reintentar"**. |

> Los estados de ubicación solo aplican si se implementa el geoetiquetado (deseable). Si no se implementa, se quitan del Figma y de la app sin afectar el resto del flujo.

---

### P4 — Revisión y finalización

**Objetivo:** ver toda la inspección de un vistazo, detectar lo que falta y finalizar. Cubre la validación del RF02 y dispara el RF03.

**Jerarquía**
1. Barra superior: atrás + "Revisión".
2. **Resumen de conteos:** tres chips con ícono, texto y número: "✓ Cumple 15", "✗ No cumple 3", "– N/A 1", más "Sin completar 1" si corresponde.
3. **Lista de todos los ítems**, en orden: número, texto (dos líneas como máximo), ícono + texto del resultado y un ícono de cámara si tiene fotos. Los ítems con problemas se marcan con ícono de advertencia.
4. **Zona del pulgar:** botón de ancho completo **"Finalizar inspección"**.

**Acciones**
| Acción | Resultado |
|---|---|
| Tocar un ítem | Vuelve a P3 en ese ítem. |
| "Finalizar inspección" | Si todo está completo: diálogo de confirmación "¿Finalizar la inspección? Después no se va a poder editar." con **Cancelar** / **Finalizar**. Al confirmar, vuelve a P1 con `Snackbar` "Inspección finalizada. Se va a subir cuando haya conexión." (o "Subiendo…" si hay red). |
| Atrás | Vuelve a P3. |

**Estados**
| Estado | Qué se ve |
|---|---|
| Contenido completo | Lista sin advertencias y botón habilitado. |
| Validación con faltantes | Al tocar "Finalizar": no finaliza. Arriba de la lista aparece una tarjeta en `notComplies`: "Faltan 2 ítems para poder finalizar", y la lista se filtra para mostrar solo los faltantes con el motivo ("Sin completar" o "No cumple sin evidencia"). Tocar uno lleva directo a ese ítem en P3. |
| Offline | Igual + `OfflineBanner`. Finalizar funciona sin conexión. |

**Por qué el botón no está deshabilitado cuando falta algo:** un botón deshabilitado no explica por qué. Dejarlo activo y responder con la lista de faltantes indica exactamente qué hacer.

---

### P5 — Detalle de inspección

**Objetivo:** consultar una inspección finalizada, ver su estado de sincronización y compartir el resumen. Cubre RF04 y la parte visible del RF03.

**Jerarquía**
1. Barra superior: atrás + nombre del sitio.
2. **Tarjeta de sincronización** (según estado, ver tabla).
3. **Datos generales:** plantilla, fecha y hora de inicio y fin.
4. **Resumen de conteos** (mismos chips que en P4).
5. **Lista de ítems** en modo lectura: texto, resultado (ícono + texto), nota y miniaturas. Primero los "No cumple", que es lo que más le importa a quien recibe el informe; después el resto, en orden.
6. **Zona del pulgar:** botón de ancho completo **"Compartir resumen"**.

**Acciones**
| Acción | Resultado |
|---|---|
| "Compartir resumen" | Abre la hoja de compartir de Android con el texto del resumen. Funciona sin conexión. |
| "Reintentar" (solo en error) | Vuelve a encolar la sincronización; la tarjeta pasa a "Pendiente". |
| Tocar una miniatura | Foto a pantalla completa. |
| Atrás | Vuelve a P1. |

**Estados**
| Estado | Tarjeta de sincronización |
|---|---|
| Carga | Indicador circular breve. |
| Pendiente | Reloj + "Pendiente de subir. Se sube sola cuando haya conexión." Si hay red y se está enviando: "Subiendo…" con barra indeterminada. |
| Sincronizada | Nube con tilde + "Sincronizada con el servidor." |
| Error | Admiración en `notComplies` + motivo en lenguaje simple + **"Reintentar"**. Ejemplos de motivo: "El servidor no respondió después de varios intentos." / "El servidor rechazó la clave de acceso de la app. Avisá al responsable." |
| Offline | `OfflineBanner`; el detalle y "Compartir" funcionan igual. |
| Error: no existe | `ErrorState`: "No encontramos esta inspección." + "Volver al inicio". Caso límite. |

**Texto del resumen compartido (ejemplo)**
```
Inspección: Control semanal de obra
Sitio: Obra Av. Siempreviva 742 — Subsuelo 2
Fecha: 25/09/2026 10:05 – 10:48
Resultado: 15 cumple · 3 no cumple · 1 N/A

No cumple:
• 7. Matafuegos señalizados y con carga vigente — Falta señalización en el matafuego del palier.
• 12. Tableros eléctricos con tapa — Tablero TS-3 sin tapa.
• 18. Vallado perimetral completo — (ver foto)

Generado con FieldCheck
```
(Las fotos no se adjuntan al texto en la v1.)

---

## 4. Criterios de accesibilidad (verificables)

| # | Criterio | Cómo se verifica |
|---|---|---|
| A1 | Todo elemento tocable mide al menos 48 × 48 dp; los botones de resultado miden 64 dp de alto. | Accessibility Scanner; medir en Figma. |
| A2 | Contraste de texto ≥ 4.5:1 y de bordes o íconos significativos ≥ 3:1, en modo claro y oscuro (tabla 1.1). | Accessibility Scanner; plugin de contraste en Figma. |
| A3 | Ningún estado se comunica solo con color: resultado, sincronización y errores llevan siempre ícono + texto. | Revisión del Figma en escala de grises. |
| A4 | Todo ícono accionable tiene descripción para TalkBack (por ejemplo, "Agregar foto al ítem 7", "Ver todos los ítems"). Las miniaturas se anuncian como "Foto 1 de 2 del ítem 7". | Recorrido con TalkBack. |
| A5 | El `ResultSelector` se anuncia como grupo de opciones con el estado seleccionado ("Cumple, seleccionado, 1 de 3"). | Recorrido con TalkBack. |
| A6 | Tras el avance automático, TalkBack anuncia el ítem nuevo ("Ítem 8 de 20: …"). Los cambios de estado de sincronización y los `Snackbar` se anuncian (región dinámica). | Recorrido con TalkBack. |
| A7 | Con el tamaño de fuente del sistema al 200 %, los textos se ajustan sin cortarse y la zona del pulgar sigue visible (el contenido superior se desplaza). | Prueba en emulador con escala de fuente máxima. |
| A8 | El flujo RF02 completo se puede hacer solo con TalkBack. | Prueba manual. |
| A9 | Modo oscuro completo para uso en interiores con poca luz. La app arranca en modo claro y se cambia con el botón de luna / sol de la barra superior; la elección se guarda entre aperturas. | Revisión visual en ambos modos; cerrar y reabrir la app con el modo oscuro activado. |
| A10 | Los `Snackbar` con acción no desaparecen solos antes de que el usuario pueda tocarlos. | Prueba manual. |

---

## 5. Textos clave (microcopy)

Tono: directo, en segunda persona (voseo), sin tecnicismos. Siempre dice **qué pasó** y **qué se puede hacer**.

| Situación | Texto |
|---|---|
| Sin conexión (banner) | Sin conexión. Tus cambios se guardan en el teléfono. |
| Inspección finalizada sin red | Inspección finalizada. Se va a subir cuando haya conexión. |
| Pendiente | Pendiente de subir. Se sube sola cuando haya conexión. |
| Sincronizada | Sincronizada con el servidor. |
| Error de sincronización (transitorio agotado) | El servidor no respondió después de varios intentos. |
| Error de sincronización (clave inválida) | El servidor rechazó la clave de acceso de la app. Avisá al responsable. |
| Sin plantillas y sin red | Necesitás conexión la primera vez. Las plantillas se descargan una sola vez y después funcionan sin señal. |
| No cumple sin evidencia | Agregá una nota o una foto como evidencia. |
| Faltantes al finalizar | Faltan N ítems para poder finalizar. |
| Foto sin ubicación | Foto guardada sin ubicación. |
| Confirmar finalización | ¿Finalizar la inspección? Después no se va a poder editar. |

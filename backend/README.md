# FieldCheck API

API REST que recibe las inspecciones sincronizadas por la app y le sirve las plantillas. FastAPI + SQLAlchemy + SQLite, empaquetada con Docker Compose.

## Cómo levantarla

### Con Docker (recomendado)

Desde esta carpeta (`backend/`):

```bash
docker compose up --build
```

- API: http://localhost:8000
- Documentación interactiva (Swagger): http://localhost:8000/docs

La base SQLite y las fotos recibidas se guardan en el volumen `fieldcheck-data`, así que sobreviven aunque se recree el contenedor. Para empezar de cero: `docker compose down -v`.

Al arrancar se crean las tablas y se cargan las 3 plantillas de ejemplo, las mismas que trae la app.

### Sin Docker

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements-dev.txt
.venv/bin/uvicorn app.main:app --reload
```

Los datos quedan en `backend/data/` (ignorada por git).

### Tests

```bash
.venv/bin/python -m pytest
```

Usan SQLite en memoria y una carpeta temporal para las fotos, así que no tocan los datos locales.

## Configuración

| Variable | Por defecto | Para qué |
|---|---|---|
| `FIELDCHECK_API_KEY` | `dev-api-key` | Clave que la app manda en el header `X-API-Key`. El valor por defecto es solo para desarrollo. |
| `DATA_DIR` | `backend/data` (en Docker: `/data`) | Carpeta de la base y las fotos. |

Con Docker, la clave se puede cambiar con un archivo `backend/.env` (no se sube al repo):

```
FIELDCHECK_API_KEY=otra-clave
```

## Endpoints

Todos menos `/health` piden el header `X-API-Key`. Sin él, o con una clave incorrecta, responden **401**.

El JSON usa camelCase, igual que los campos de la app. Las fechas son milisegundos desde epoch.

| Método y ruta | Qué hace | Respuestas |
|---|---|---|
| `GET /health` | Indica si el servidor está levantado. | 200 |
| `GET /templates` | Plantillas con su `version` y sus ítems ordenados. La app compara la versión para saber si una plantilla cambió. | 200 |
| `POST /inspections` | Recibe una inspección finalizada con sus ítems. Es idempotente por `clientUuid` (ver abajo). | 201 creada · 200 ya existía · 422 datos inválidos |
| `GET /inspections/{client_uuid}` | Devuelve la inspección tal como quedó en el servidor, con las fotos agrupadas por ítem. | 200 · 404 |
| `POST /inspections/{client_uuid}/evidence` | Sube una foto de un ítem (multipart). Es idempotente por el `clientUuid` de la foto. | 201 subida · 200 ya existía · 404 · 409 · 413 · 415 · 422 |

### Ejemplo: enviar una inspección

```json
POST /inspections
{
  "clientUuid": "6f1c2a9e-0000-4000-8000-000000000001",
  "templateName": "Control semanal de obra",
  "templateVersion": 3,
  "siteName": "Obra Av. Siempreviva 742",
  "status": "FINISHED",
  "startedAt": 1790000000000,
  "finishedAt": 1790003600000,
  "updatedAt": 1790003600000,
  "items": [
    { "position": 1, "itemText": "El vallado está completo", "result": "COMPLIES" },
    { "position": 2, "itemText": "Los tableros tienen tapa", "result": "NOT_COMPLIES", "note": "TS-3 sin tapa" }
  ]
}
```

Reglas de validación (si no se cumplen: **422**):

- `status` tiene que ser `FINISHED`, porque las inspecciones en curso no se sincronizan.
- Todos los ítems tienen `result` (`COMPLIES`, `NOT_COMPLIES` o `NOT_APPLICABLE`) y no se repiten posiciones.
- `finishedAt` no puede ser anterior a `startedAt`.
- Los `clientUuid` solo admiten letras, números y guiones.

### Ejemplo: subir una foto

```bash
curl -H "X-API-Key: dev-api-key" \
  -F "file=@foto.jpg;type=image/jpeg" \
  -F clientUuid=8d2b7c41-0000-4000-8000-000000000001 \
  -F itemPosition=2 \
  -F capturedAt=1790001000000 \
  http://localhost:8000/inspections/6f1c2a9e-0000-4000-8000-000000000001/evidence
```

Campos opcionales: `latitude` y `longitude`.

| Respuesta | Cuándo |
|---|---|
| 404 | La inspección no se subió todavía (primero datos, después fotos). |
| 409 | Ese `clientUuid` de foto ya pertenece a otra inspección. |
| 413 | La foto supera los 10 MB. |
| 415 | La foto no es JPEG ni PNG. |
| 422 | Archivo vacío, o no hay un ítem en esa posición. |

## Decisiones

- **Idempotencia por `clientUuid`.** El teléfono genera el id de cada inspección y de cada foto, y el servidor lo usa como clave primaria. Si se corta la conexión después de guardar y la app reintenta, el reenvío encuentra la misma fila y no duplica nada. Con una inspección existente:
  - Si el `updatedAt` que llega es mayor, la inspección se actualiza (*last-write-wins*).
  - Si es igual o menor, se devuelve lo guardado sin cambios.
- **Ítems por posición.** En la app el id de un ítem es un número local de Room que el servidor no conoce. Por eso en el servidor un ítem se identifica por (inspección, posición), y las fotos indican `itemPosition`. Si la inspección se reenvía, las fotos ya subidas no se pierden.
- **Las fotos no van en la base.** Se guardan en `<DATA_DIR>/evidence/<inspección>/<foto>.jpg` y la base solo guarda la ruta.
- **Errores sin stack traces.** Un error inesperado devuelve `500 {"detail": "Error interno del servidor"}`, y el detalle queda en el log del contenedor.
- **Sin migraciones (por ahora).** Las tablas se crean al arrancar. Si cambia el modelo, se borra el volumen.

## Conectar la app desde el emulador

El emulador de Android ve a la computadora como `10.0.2.2` (`localhost` sería el propio emulador). Con la API levantada en Docker, la URL base de la app es:

```
http://10.0.2.2:8000/
```

Como es HTTP sin TLS, la app solo lo permite para ese host en las compilaciones de debug (network security config). Desde un celular físico en la misma red Wi-Fi se usa la IP local de la computadora.

Para probar rápido desde el emulador, abrir `http://10.0.2.2:8000/health` en el navegador del emulador.

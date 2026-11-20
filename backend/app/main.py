import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app import models  # noqa: F401  (registra las tablas en Base.metadata)
from app.config import DATA_DIR, EVIDENCE_DIR
from app.db import Base, SessionLocal, engine
from app.routers import health, inspections, templates
from app.seed import seed_templates

logger = logging.getLogger("fieldcheck")


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Al arrancar: carpetas, tablas y plantillas de ejemplo.
    # Sin Alembic: si cambia el modelo, por ahora se borra el volumen y se vuelve a crear.
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    EVIDENCE_DIR.mkdir(parents=True, exist_ok=True)
    Base.metadata.create_all(engine)
    with SessionLocal() as db:
        seed_templates(db)
    yield


app = FastAPI(title="FieldCheck API", version="1.0.0", lifespan=lifespan)
app.include_router(health.router)
app.include_router(templates.router)
app.include_router(inspections.router)


# Si algo falla adentro, el cliente recibe un mensaje genérico y el detalle queda en el log del servidor
@app.exception_handler(Exception)
async def unexpected_error(request: Request, exc: Exception):
    logger.exception("Error inesperado en %s %s", request.method, request.url.path)
    return JSONResponse(status_code=500, content={"detail": "Error interno del servidor"})

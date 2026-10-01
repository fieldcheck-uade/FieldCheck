import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.routers import health

logger = logging.getLogger("fieldcheck")

app = FastAPI(title="FieldCheck API", version="1.0.0")
app.include_router(health.router)


# Si algo falla adentro, el cliente recibe un mensaje genérico y el detalle queda en el log del servidor
@app.exception_handler(Exception)
async def unexpected_error(request: Request, exc: Exception):
    logger.exception("Error inesperado en %s %s", request.method, request.url.path)
    return JSONResponse(status_code=500, content={"detail": "Error interno del servidor"})

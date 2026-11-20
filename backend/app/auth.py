import secrets

from fastapi import Header, HTTPException

from app import config


# Protección mínima para una v1 sin usuarios: una sola clave compartida en el header X-API-Key.
# La app trata el 401 como error permanente ("API key inválida") y no reintenta sola.
def require_api_key(x_api_key: str | None = Header(default=None)):
    if x_api_key is None or not secrets.compare_digest(x_api_key, config.API_KEY):
        raise HTTPException(status_code=401, detail="API key inválida o ausente")

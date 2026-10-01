from fastapi import APIRouter

router = APIRouter(tags=["health"])


# Público (sin API key): sirve para ver rápido si el servidor está levantado
@router.get("/health")
def health():
    return {"status": "ok"}

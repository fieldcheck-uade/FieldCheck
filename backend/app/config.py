import os
from pathlib import Path

# Todo se configura con variables de entorno, así no hay rutas ni claves escritas en el código.
# En Docker, DATA_DIR apunta al volumen; corriendo local queda en backend/data.
DATA_DIR = Path(os.getenv("DATA_DIR", Path(__file__).resolve().parent.parent / "data"))
DATABASE_URL = os.getenv("DATABASE_URL", f"sqlite:///{DATA_DIR / 'fieldcheck.db'}")
EVIDENCE_DIR = Path(os.getenv("EVIDENCE_DIR", DATA_DIR / "evidence"))

# La app la manda en el header X-API-Key. El valor por defecto es solo para desarrollo local.
API_KEY = os.getenv("FIELDCHECK_API_KEY", "dev-api-key")

MAX_PHOTO_BYTES = 10 * 1024 * 1024  # 10 MB; la app las redimensiona a ~1 MB (RNF07)

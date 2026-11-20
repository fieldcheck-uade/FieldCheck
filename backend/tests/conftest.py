import os
import tempfile

import pytest

# Se configura antes de importar la app: base en memoria y una carpeta temporal para las fotos
os.environ["DATABASE_URL"] = "sqlite://"
os.environ["DATA_DIR"] = tempfile.mkdtemp()
os.environ["FIELDCHECK_API_KEY"] = "test-key"

from fastapi.testclient import TestClient  # noqa: E402

from app import config  # noqa: E402
from app.db import Base, engine  # noqa: E402
from app.main import app  # noqa: E402


@pytest.fixture
def evidence_dir(tmp_path, monkeypatch):
    monkeypatch.setattr(config, "EVIDENCE_DIR", tmp_path)
    return tmp_path


@pytest.fixture
def client(evidence_dir):
    # Base limpia en cada test; al entrar al "with" corre el arranque de la app (tablas + plantillas)
    Base.metadata.drop_all(engine)
    with TestClient(app, headers={"X-API-Key": "test-key"}) as test_client:
        yield test_client

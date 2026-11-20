import copy

from fastapi.testclient import TestClient

from app.db import get_db
from app.main import app

INSPECTION = {
    "clientUuid": "6f1c2a9e-0000-4000-8000-000000000001",
    "templateName": "Control semanal de obra",
    "templateVersion": 3,
    "siteName": "Obra Av. Siempreviva 742",
    "status": "FINISHED",
    "startedAt": 1_000,
    "finishedAt": 2_000,
    "updatedAt": 2_000,
    "items": [
        {"position": 1, "itemText": "El vallado está completo", "result": "COMPLIES"},
        {"position": 2, "itemText": "Los tableros tienen tapa", "result": "NOT_COMPLIES", "note": "TS-3 sin tapa"},
    ],
}
UUID = INSPECTION["clientUuid"]
JPEG = b"\xff\xd8\xff\xe0" + b"foto de prueba"


def inspection(**changes):
    data = copy.deepcopy(INSPECTION)
    data.update(changes)
    return data


def upload_photo(client, photo_uuid="foto-1", position=2, inspection_uuid=UUID, content_type="image/jpeg", data=JPEG):
    return client.post(
        f"/inspections/{inspection_uuid}/evidence",
        files={"file": ("foto.jpg", data, content_type)},
        data={"clientUuid": photo_uuid, "itemPosition": str(position), "capturedAt": "1500"},
    )


# ---- Salud y API key ----

def test_health_is_public(client):
    response = client.get("/health", headers={"X-API-Key": ""})
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_requests_without_valid_api_key_get_401(client):
    assert client.get("/templates", headers={"X-API-Key": "otra"}).status_code == 401
    without_header = TestClient(app).get("/templates")
    assert without_header.status_code == 401
    assert without_header.json() == {"detail": "API key inválida o ausente"}


# ---- Plantillas ----

def test_templates_are_seeded_with_version_and_ordered_items(client):
    response = client.get("/templates")

    assert response.status_code == 200
    templates = {t["id"]: t for t in response.json()}
    assert set(templates) == {"tpl-obra", "tpl-deposito", "tpl-mant"}
    obra = templates["tpl-obra"]
    assert obra["version"] == 3
    assert len(obra["items"]) == 10
    assert [i["position"] for i in obra["items"]] == list(range(1, 11))
    assert obra["items"][0] == {
        "id": "tpl-obra-1",
        "text": "El vallado perimetral está completo y en buen estado",
        "position": 1,
    }


# ---- Inspecciones ----

def test_create_inspection_returns_201_and_can_be_read(client):
    created = client.post("/inspections", json=INSPECTION)

    assert created.status_code == 201
    body = created.json()
    assert body["clientUuid"] == UUID
    assert [i["result"] for i in body["items"]] == ["COMPLIES", "NOT_COMPLIES"]
    assert body["items"][1]["note"] == "TS-3 sin tapa"

    read = client.get(f"/inspections/{UUID}")
    assert read.status_code == 200
    assert read.json() == body


def test_sending_the_same_inspection_twice_does_not_duplicate(client):
    first = client.post("/inspections", json=INSPECTION)
    second = client.post("/inspections", json=INSPECTION)

    assert first.status_code == 201
    assert second.status_code == 200
    assert second.json() == first.json()  # mismo recurso, incluido receivedAt
    assert len(client.get(f"/inspections/{UUID}").json()["items"]) == 2


def test_newer_version_replaces_the_inspection(client):
    client.post("/inspections", json=INSPECTION)
    newer = inspection(updatedAt=3_000, siteName="Obra corregida")
    newer["items"][1]["note"] = "Tablero TS-3 sin tapa, avisado al capataz"

    response = client.post("/inspections", json=newer)

    assert response.status_code == 200
    assert response.json()["siteName"] == "Obra corregida"
    assert response.json()["items"][1]["note"] == "Tablero TS-3 sin tapa, avisado al capataz"


def test_older_version_is_ignored(client):
    client.post("/inspections", json=inspection(updatedAt=5_000))

    response = client.post("/inspections", json=inspection(updatedAt=4_000, siteName="Versión vieja"))

    assert response.status_code == 200
    assert response.json()["siteName"] == INSPECTION["siteName"]
    assert response.json()["updatedAt"] == 5_000


def test_invalid_payloads_get_422(client):
    item_without_result = inspection()
    del item_without_result["items"][0]["result"]
    repeated_position = inspection()
    repeated_position["items"][1]["position"] = 1

    invalid = [
        inspection(status="IN_PROGRESS"),       # solo se suben finalizadas
        item_without_result,
        repeated_position,
        inspection(items=[]),
        inspection(finishedAt=500),             # termina antes de empezar
        inspection(clientUuid="../../etc"),     # caracteres no permitidos
        inspection(siteName=""),
    ]
    for payload in invalid:
        response = client.post("/inspections", json=payload)
        assert response.status_code == 422, payload
        assert "detail" in response.json()

    assert client.get(f"/inspections/{UUID}").status_code == 404  # no se guardó nada


def test_unknown_inspection_gets_404(client):
    response = client.get("/inspections/no-existe")
    assert response.status_code == 404
    assert response.json() == {"detail": "La inspección no existe"}


# ---- Fotos ----

def test_upload_photo_saves_the_file_and_links_it_to_the_item(client, evidence_dir):
    client.post("/inspections", json=INSPECTION)

    response = upload_photo(client)

    assert response.status_code == 201
    assert response.json()["clientUuid"] == "foto-1"
    assert (evidence_dir / UUID / "foto-1.jpg").read_bytes() == JPEG
    items = client.get(f"/inspections/{UUID}").json()["items"]
    assert [e["clientUuid"] for e in items[1]["evidence"]] == ["foto-1"]
    assert items[0]["evidence"] == []


def test_uploading_the_same_photo_twice_does_not_duplicate(client):
    client.post("/inspections", json=INSPECTION)

    first = upload_photo(client)
    second = upload_photo(client)

    assert first.status_code == 201
    assert second.status_code == 200
    assert second.json() == first.json()
    evidence = client.get(f"/inspections/{UUID}").json()["items"][1]["evidence"]
    assert len(evidence) == 1


def test_photos_survive_a_newer_version_of_the_inspection(client):
    client.post("/inspections", json=INSPECTION)
    upload_photo(client)

    client.post("/inspections", json=inspection(updatedAt=3_000))

    evidence = client.get(f"/inspections/{UUID}").json()["items"][1]["evidence"]
    assert [e["clientUuid"] for e in evidence] == ["foto-1"]


def test_photo_uuid_used_by_another_inspection_gets_409(client):
    client.post("/inspections", json=INSPECTION)
    client.post("/inspections", json=inspection(clientUuid="otra-inspeccion"))
    upload_photo(client)

    response = upload_photo(client, inspection_uuid="otra-inspeccion")

    assert response.status_code == 409


def test_photo_errors(client):
    client.post("/inspections", json=INSPECTION)

    assert upload_photo(client, inspection_uuid="no-existe").status_code == 404
    assert upload_photo(client, position=99).status_code == 422
    assert upload_photo(client, content_type="application/pdf").status_code == 415
    assert upload_photo(client, data=b"").status_code == 422
    assert upload_photo(client, photo_uuid="../escape").status_code == 422


# ---- Errores inesperados ----

def test_unexpected_errors_do_not_leak_details(client):
    def broken_db():
        raise RuntimeError("detalle interno que no tiene que llegar al cliente")

    app.dependency_overrides[get_db] = broken_db
    try:
        response = TestClient(app, raise_server_exceptions=False).get(
            "/templates", headers={"X-API-Key": "test-key"}
        )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 500
    assert response.json() == {"detail": "Error interno del servidor"}

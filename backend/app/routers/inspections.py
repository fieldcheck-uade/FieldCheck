import time
from typing import Annotated

from fastapi import APIRouter, Depends, File, Form, HTTPException, Path, Response, UploadFile, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app import config
from app.auth import require_api_key
from app.db import get_db
from app.models import Evidence, Inspection, ItemResult
from app.schemas import ClientUuid, EvidenceOut, InspectionIn, InspectionOut, ItemResultOut

router = APIRouter(tags=["inspections"], dependencies=[Depends(require_api_key)])

UuidPath = Annotated[str, Path(pattern=r"^[A-Za-z0-9-]{1,64}$")]

ALLOWED_PHOTO_TYPES = {"image/jpeg": "jpg", "image/png": "png"}


def now_ms() -> int:
    return int(time.time() * 1000)


def get_inspection_or_404(db: Session, client_uuid: str) -> Inspection:
    inspection = db.get(Inspection, client_uuid)
    if inspection is None:
        raise HTTPException(status_code=404, detail="La inspección no existe")
    return inspection


def to_out(inspection: Inspection) -> InspectionOut:
    # Las fotos se guardan por posición de ítem; acá se agrupan dentro de cada ítem
    items = [
        ItemResultOut(
            position=item.position,
            item_text=item.item_text,
            result=item.result,
            note=item.note,
            evidence=[EvidenceOut.model_validate(e) for e in inspection.evidence if e.item_position == item.position],
        )
        for item in inspection.items
    ]
    return InspectionOut(
        client_uuid=inspection.client_uuid,
        template_name=inspection.template_name,
        template_version=inspection.template_version,
        site_name=inspection.site_name,
        status=inspection.status,
        started_at=inspection.started_at,
        finished_at=inspection.finished_at,
        updated_at=inspection.updated_at,
        received_at=inspection.received_at,
        items=items,
    )


def new_items(payload: InspectionIn) -> list[ItemResult]:
    return [
        ItemResult(position=i.position, item_text=i.item_text, result=i.result.value, note=i.note)
        for i in payload.items
    ]


# Idempotente por clientUuid (lo genera el teléfono, ver diagrama de sincronización):
# - no existe              -> se crea (201)
# - existe y llega más nueva (updatedAt mayor) -> se actualiza (200)
# - existe y llega igual o más vieja -> no se toca, se devuelve lo guardado (200)
# Así, si se cortó la conexión antes de la respuesta, el reintento no duplica nada.
@router.post("/inspections", response_model=InspectionOut)
def upsert_inspection(payload: InspectionIn, response: Response, db: Session = Depends(get_db)):
    existing = db.get(Inspection, payload.client_uuid)

    if existing is None:
        inspection = Inspection(
            client_uuid=payload.client_uuid,
            template_name=payload.template_name,
            template_version=payload.template_version,
            site_name=payload.site_name,
            status=payload.status,
            started_at=payload.started_at,
            finished_at=payload.finished_at,
            updated_at=payload.updated_at,
            received_at=now_ms(),
            items=new_items(payload),
        )
        db.add(inspection)
        try:
            db.commit()
        except IntegrityError:
            # Dos envíos iguales al mismo tiempo: el otro ganó, devolvemos lo que quedó guardado
            db.rollback()
            return to_out(get_inspection_or_404(db, payload.client_uuid))
        response.status_code = status.HTTP_201_CREATED
        return to_out(inspection)

    if payload.updated_at > existing.updated_at:
        existing.template_name = payload.template_name
        existing.template_version = payload.template_version
        existing.site_name = payload.site_name
        existing.status = payload.status
        existing.started_at = payload.started_at
        existing.finished_at = payload.finished_at
        existing.updated_at = payload.updated_at
        existing.received_at = now_ms()
        # Se borran los ítems viejos antes de insertar los nuevos, por la restricción única (inspección, posición)
        existing.items.clear()
        db.flush()
        existing.items = new_items(payload)
        db.commit()

    return to_out(existing)


@router.get("/inspections/{client_uuid}", response_model=InspectionOut)
def get_inspection(client_uuid: UuidPath, db: Session = Depends(get_db)):
    return to_out(get_inspection_or_404(db, client_uuid))


# Sube una foto de un ítem. Idempotente por el clientUuid de la foto:
# si ya se había subido para esta inspección, se devuelve la existente sin guardar el archivo de nuevo.
@router.post("/inspections/{client_uuid}/evidence", response_model=EvidenceOut, status_code=201)
def upload_evidence(
    client_uuid: UuidPath,
    response: Response,
    file: Annotated[UploadFile, File()],
    evidence_uuid: Annotated[ClientUuid, Form(alias="clientUuid")],
    item_position: Annotated[int, Form(alias="itemPosition", ge=1)],
    captured_at: Annotated[int, Form(alias="capturedAt", ge=0)],
    latitude: Annotated[float | None, Form(ge=-90, le=90)] = None,
    longitude: Annotated[float | None, Form(ge=-180, le=180)] = None,
    db: Session = Depends(get_db),
):
    inspection = get_inspection_or_404(db, client_uuid)

    existing = db.get(Evidence, evidence_uuid)
    if existing is not None:
        if existing.inspection_uuid != client_uuid:
            raise HTTPException(status_code=409, detail="Esa foto ya pertenece a otra inspección")
        response.status_code = status.HTTP_200_OK
        return existing

    if item_position not in {item.position for item in inspection.items}:
        raise HTTPException(status_code=422, detail=f"La inspección no tiene un ítem en la posición {item_position}")

    extension = ALLOWED_PHOTO_TYPES.get(file.content_type or "")
    if extension is None:
        raise HTTPException(status_code=415, detail="La foto tiene que ser JPEG o PNG")

    # Se lee un byte más que el máximo para saber si se pasó sin cargar archivos enormes en memoria
    data = file.file.read(config.MAX_PHOTO_BYTES + 1)
    if len(data) == 0:
        raise HTTPException(status_code=422, detail="El archivo está vacío")
    if len(data) > config.MAX_PHOTO_BYTES:
        raise HTTPException(status_code=413, detail="La foto supera los 10 MB")

    # Una carpeta por inspección. Los ids solo tienen letras, números y guiones, así que la ruta es segura.
    relative_path = f"{client_uuid}/{evidence_uuid}.{extension}"
    destination = config.EVIDENCE_DIR / relative_path
    destination.parent.mkdir(parents=True, exist_ok=True)
    # Se escribe a un temporal y después se renombra, para no dejar archivos a medias si algo falla
    temporary = destination.with_suffix(".part")
    temporary.write_bytes(data)
    temporary.replace(destination)

    evidence = Evidence(
        client_uuid=evidence_uuid,
        inspection_uuid=client_uuid,
        item_position=item_position,
        file_path=relative_path,
        content_type=file.content_type,
        captured_at=captured_at,
        latitude=latitude,
        longitude=longitude,
        received_at=now_ms(),
    )
    db.add(evidence)
    try:
        db.commit()
    except IntegrityError:
        # La misma foto llegó dos veces al mismo tiempo: devolvemos la que quedó guardada
        db.rollback()
        response.status_code = status.HTTP_200_OK
        return db.get(Evidence, evidence_uuid)
    return evidence

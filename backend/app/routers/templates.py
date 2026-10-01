from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.auth import require_api_key
from app.db import get_db
from app.models import Template
from app.schemas import TemplateOut

router = APIRouter(tags=["templates"], dependencies=[Depends(require_api_key)])


# La app guarda la respuesta como caché y compara "version" para saber si una plantilla cambió
@router.get("/templates", response_model=list[TemplateOut])
def list_templates(db: Session = Depends(get_db)):
    query = select(Template).options(selectinload(Template.items)).order_by(Template.name)
    return db.scalars(query).all()

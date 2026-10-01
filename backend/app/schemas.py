from enum import Enum
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator
from pydantic.alias_generators import to_camel

# El JSON usa camelCase, igual que los campos de Room y del dominio en la app,
# así los DTO de Kotlin no necesitan @SerialName. En Python se escribe en snake_case.

# Ids generados en el teléfono. Se limitan a letras, números y guiones porque
# también se usan para armar la ruta de las fotos en disco.
ClientUuid = Annotated[str, Field(pattern=r"^[A-Za-z0-9-]{1,64}$")]


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, from_attributes=True)


class ItemStatus(str, Enum):
    COMPLIES = "COMPLIES"
    NOT_COMPLIES = "NOT_COMPLIES"
    NOT_APPLICABLE = "NOT_APPLICABLE"


# ---- Plantillas ----

class TemplateItemOut(CamelModel):
    id: str
    text: str
    position: int


class TemplateOut(CamelModel):
    id: str
    name: str
    version: int
    items: list[TemplateItemOut]


# ---- Inspecciones (lo que manda la app) ----

class ItemResultIn(CamelModel):
    position: int = Field(ge=1)
    item_text: str = Field(min_length=1)
    # Solo se sincronizan inspecciones finalizadas, así que todos los ítems tienen resultado
    result: ItemStatus
    note: str | None = None


class InspectionIn(CamelModel):
    client_uuid: ClientUuid
    template_name: str = Field(min_length=1)
    template_version: int = Field(ge=1)
    site_name: str = Field(min_length=1, max_length=200)
    status: Literal["FINISHED"]  # una inspección en curso todavía no se sube
    started_at: int = Field(ge=0)
    finished_at: int = Field(ge=0)
    updated_at: int = Field(ge=0)
    items: list[ItemResultIn] = Field(min_length=1)

    @model_validator(mode="after")
    def check_consistency(self):
        if self.finished_at < self.started_at:
            raise ValueError("finishedAt no puede ser anterior a startedAt")
        positions = [item.position for item in self.items]
        if len(positions) != len(set(positions)):
            raise ValueError("Hay ítems con la misma posición")
        return self


# ---- Inspecciones (lo que devuelve el servidor) ----

class EvidenceOut(CamelModel):
    client_uuid: str
    item_position: int
    content_type: str
    captured_at: int
    latitude: float | None
    longitude: float | None
    received_at: int


class ItemResultOut(CamelModel):
    position: int
    item_text: str
    result: ItemStatus
    note: str | None
    evidence: list[EvidenceOut] = []


class InspectionOut(CamelModel):
    client_uuid: str
    template_name: str
    template_version: int
    site_name: str
    status: str
    started_at: int
    finished_at: int
    updated_at: int
    received_at: int
    items: list[ItemResultOut]

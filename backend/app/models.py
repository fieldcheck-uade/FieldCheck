from sqlalchemy import BigInteger, Float, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base

# Las fechas son milisegundos desde epoch, igual que en la app (System.currentTimeMillis()).


class Template(Base):
    __tablename__ = "templates"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    name: Mapped[str] = mapped_column(String)
    # La app compara la versión para saber si la plantilla cambió
    version: Mapped[int] = mapped_column(Integer)

    items: Mapped[list["TemplateItem"]] = relationship(
        back_populates="template", cascade="all, delete-orphan", order_by="TemplateItem.position"
    )


class TemplateItem(Base):
    __tablename__ = "template_items"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    template_id: Mapped[str] = mapped_column(ForeignKey("templates.id", ondelete="CASCADE"), index=True)
    text: Mapped[str] = mapped_column(Text)
    position: Mapped[int] = mapped_column(Integer)

    template: Mapped[Template] = relationship(back_populates="items")


class Inspection(Base):
    __tablename__ = "inspections"

    # Generado en el teléfono. Es la clave de idempotencia: el mismo envío dos veces es la misma fila.
    client_uuid: Mapped[str] = mapped_column(String, primary_key=True)
    template_name: Mapped[str] = mapped_column(String)
    template_version: Mapped[int] = mapped_column(Integer)
    site_name: Mapped[str] = mapped_column(String)
    status: Mapped[str] = mapped_column(String)
    started_at: Mapped[int] = mapped_column(BigInteger)
    finished_at: Mapped[int] = mapped_column(BigInteger)
    # updated_at viene de la app y decide si un envío pisa al anterior (last-write-wins)
    updated_at: Mapped[int] = mapped_column(BigInteger)
    received_at: Mapped[int] = mapped_column(BigInteger)  # hora del servidor, para auditar

    items: Mapped[list["ItemResult"]] = relationship(
        back_populates="inspection", cascade="all, delete-orphan", order_by="ItemResult.position"
    )
    evidence: Mapped[list["Evidence"]] = relationship(
        back_populates="inspection", cascade="all, delete-orphan", order_by="Evidence.captured_at"
    )


class ItemResult(Base):
    __tablename__ = "item_results"
    # En el teléfono el id del ítem es un Long local que el servidor no conoce.
    # Por eso acá un ítem se identifica por (inspección, posición).
    __table_args__ = (UniqueConstraint("inspection_uuid", "position"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    inspection_uuid: Mapped[str] = mapped_column(
        ForeignKey("inspections.client_uuid", ondelete="CASCADE"), index=True
    )
    position: Mapped[int] = mapped_column(Integer)
    item_text: Mapped[str] = mapped_column(Text)
    result: Mapped[str] = mapped_column(String)
    note: Mapped[str | None] = mapped_column(Text, nullable=True)

    inspection: Mapped[Inspection] = relationship(back_populates="items")


class Evidence(Base):
    __tablename__ = "evidence"

    # También generado en el teléfono: subir la misma foto dos veces no la duplica
    client_uuid: Mapped[str] = mapped_column(String, primary_key=True)
    inspection_uuid: Mapped[str] = mapped_column(
        ForeignKey("inspections.client_uuid", ondelete="CASCADE"), index=True
    )
    # Apunta al ítem por posición y no por FK: así, si la inspección se reenvía y se
    # reemplazan sus ítems, las fotos ya subidas no se pierden.
    item_position: Mapped[int] = mapped_column(Integer)
    file_path: Mapped[str] = mapped_column(String)  # relativo a EVIDENCE_DIR; la foto no va en la base
    content_type: Mapped[str] = mapped_column(String)
    captured_at: Mapped[int] = mapped_column(BigInteger)
    latitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    longitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    received_at: Mapped[int] = mapped_column(BigInteger)

    inspection: Mapped[Inspection] = relationship(back_populates="evidence")

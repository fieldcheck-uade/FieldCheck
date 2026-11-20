from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import Template, TemplateItem

# Las mismas plantillas que carga la app (FakeData.kt), con los mismos ids,
# para que la app y el servidor hablen de lo mismo mientras no haya un ABM de plantillas.
TEMPLATES = [
    (
        "tpl-obra",
        "Control semanal de obra",
        3,
        [
            "El vallado perimetral está completo y en buen estado",
            "La cartelería de obra y de seguridad está visible",
            "Los trabajadores usan casco y calzado de seguridad",
            "Los accesos y circulaciones están libres de obstáculos",
            "Las escaleras y andamios están firmes y con barandas",
            "Los materiales están acopiados en forma ordenada",
            "Los matafuegos están señalizados y con la carga vigente",
            "El botiquín de primeros auxilios está completo",
            "Los residuos se separan en los contenedores indicados",
            "Los tableros eléctricos tienen tapa y señalización",
        ],
    ),
    (
        "tpl-deposito",
        "Seguridad e higiene en depósito",
        2,
        [
            "Las salidas de emergencia están despejadas y señalizadas",
            "La iluminación de emergencia funciona",
            "Las estanterías están ancladas y sin sobrecarga",
            "Los pasillos tienen el ancho mínimo libre",
            "Hay registro actualizado de control de plagas",
            "Los productos químicos están rotulados y separados",
        ],
    ),
    (
        "tpl-mant",
        "Recorrida de mantenimiento edilicio",
        1,
        [
            "No hay filtraciones ni manchas de humedad en techos",
            "Los ascensores tienen la habilitación vigente",
            "Las bombas de agua funcionan sin ruidos extraños",
            "Las luminarias de espacios comunes funcionan",
            "Las puertas cortafuego cierran solas",
        ],
    ),
]


def seed_templates(db: Session) -> None:
    # Solo la primera vez: si ya hay plantillas no se toca nada
    if db.scalars(select(Template).limit(1)).first() is not None:
        return
    for template_id, name, version, texts in TEMPLATES:
        db.add(
            Template(
                id=template_id,
                name=name,
                version=version,
                # Mismo formato de id que la app: "<plantilla>-<posición>"
                items=[
                    TemplateItem(id=f"{template_id}-{i}", text=text, position=i)
                    for i, text in enumerate(texts, start=1)
                ],
            )
        )
    db.commit()

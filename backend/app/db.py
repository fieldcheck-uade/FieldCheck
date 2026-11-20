from sqlalchemy import create_engine, event
from sqlalchemy.orm import DeclarativeBase, sessionmaker
from sqlalchemy.pool import StaticPool

from app.config import DATABASE_URL


class Base(DeclarativeBase):
    pass


def make_engine(url: str):
    # Base en memoria (tests): todas las sesiones tienen que compartir la misma conexión
    extra = {"poolclass": StaticPool} if url in ("sqlite://", "sqlite:///:memory:") else {}
    # check_same_thread=False: FastAPI atiende requests en distintos hilos con la misma conexión de SQLite
    engine = create_engine(url, connect_args={"check_same_thread": False}, **extra)

    # SQLite trae las claves foráneas apagadas: hay que prenderlas en cada conexión para que funcione el CASCADE
    @event.listens_for(engine, "connect")
    def enable_foreign_keys(dbapi_connection, _):
        dbapi_connection.execute("PRAGMA foreign_keys = ON")

    return engine


engine = make_engine(DATABASE_URL)
SessionLocal = sessionmaker(bind=engine, autoflush=False)


# Dependencia de FastAPI: una sesión por request, que se cierra siempre al terminar
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

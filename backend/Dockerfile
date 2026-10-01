FROM python:3.14-slim

WORKDIR /code

# Primero las dependencias, así Docker las cachea si solo cambia el código
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# La base y las fotos van al volumen montado en /data (ver docker-compose.yml)
ENV DATA_DIR=/data
EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

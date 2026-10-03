FROM python:3.12-slim

# tzdata: para que RUN_AT respete la variable TZ.
RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY vigia ./vigia

RUN useradd --create-home vigia
USER vigia

CMD ["python", "-m", "vigia", "schedule"]

# João Pedro G M Silva - PUC Goiás ADS - 20251012000740
FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY requirements.txt ./
# `requirements.txt` já inclui pytest + httpx para que o step do CI
# ("Run backend stub contract tests") rode sem instalar nada extra.
RUN pip install --no-cache-dir -r requirements.txt

COPY server.py ./
COPY tests/ ./tests/

EXPOSE 8000

CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8000"]
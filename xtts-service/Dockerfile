# XTTS voice cloning service with Coqui TTS (Python 3.11).
# Build: docker build -t xtts-service .
# Run:   docker run -p 127.0.0.1:9000:9000 -e XTTS_API_KEY=<secret> xtts-service
# Backend sets XTTS_BASE_URL=http://<host>:9000 and the same XTTS_API_KEY.
# The container listens on 0.0.0.0 inside its own network namespace; never publish port 9000 on a public interface —
# keep it on localhost or a private network reachable only by the backend.

FROM python:3.11-slim

WORKDIR /app

# Install system deps for audio (optional, for some codecs)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt requirements-xtts.txt ./
RUN pip install --no-cache-dir -r requirements.txt -r requirements-xtts.txt

COPY main.py tamixa.mp3 ./

EXPOSE 9000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "9000"]

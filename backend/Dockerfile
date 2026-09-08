FROM python:3.12-slim
WORKDIR /app
COPY backend/jarvis_backend.py /app/jarvis_backend.py
ENV PYTHONUNBUFFERED=1
EXPOSE 8080
CMD ["python","/app/jarvis_backend.py"]

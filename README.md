# EXE201-MosiacStore_Backend
Here’s a beginner-friendly, standalone README.md you can place in your GitHub repo so that anyone — even non-developers — can follow it to run your app from Docker Hub:

markdown
Copy
Edit
# 🌍 Vietshirt QR - Docker Quick Start Guide

This guide helps **anyone** (even if you're not a developer) run the Vietshirt QR web application using Docker.

You do **NOT** need:
- Any code
- Any programming skills
- Any account on GitHub

✅ All you need is **Docker Desktop** installed!

---

## 🧰 Requirements

1. ✅ Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
   - Windows: Works best on Windows 10/11
   - macOS: Works on Intel/M1/M2 chip (make sure to allow Docker permissions)

2. ✅ Basic knowledge of using Terminal (macOS/Linux) or PowerShell (Windows)

---

## 🚀 Quick Start in 3 Steps

### ✅ Step 1: Create a `docker-compose.yml` file

Create a new empty folder (anywhere on your computer). Inside it, create a file named:

docker-compose.yml

yaml
Copy
Edit

Paste the following content:

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: vietshirt-postgres
    environment:
      POSTGRES_USER: vietshirt
      POSTGRES_PASSWORD: vietshirt_password
      POSTGRES_DB: vietshirt_db
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U vietshirt"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - vietshirt-network

  # MinIO Object Storage
  minio:
    image: minio/minio
    container_name: vietshirt-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minio_user
      MINIO_ROOT_PASSWORD: minio_password
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    networks:
      - vietshirt-network

  # Spring Boot Application từ Docker Hub
  app:
    image: khoa2486/vietshirt-qr:latest
    container_name: vietshirt-app
    depends_on:
      postgres:
        condition: service_healthy
      minio:
        condition: service_started
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/vietshirt_db
      - SPRING_DATASOURCE_USERNAME=vietshirt
      - SPRING_DATASOURCE_PASSWORD=vietshirt_password
      - MINIO_ENDPOINT=minio:9000
      - MINIO_ACCESS_KEY=minio_user
      - MINIO_SECRET_KEY=minio_password
    networks:
      - vietshirt-network

networks:
  vietshirt-network:
    driver: bridge

volumes:
  postgres_data:
  minio_data:

✅ Step 2: Open Terminal or PowerShell
In that folder (where docker-compose.yml is saved):

bash
Copy
Edit
# Pull the latest app image from DockerHub
docker pull khoa2486/vietshirt-qr:latest

# Start the app and services in the background
docker compose up -d
✅ Step 3: Access the App
Open your browser and go to:

arduino
Copy
Edit
http://localhost:8080
You should now see the Vietshirt QR app running 🚀

💡 FAQs
Q: I'm using macOS M1 and see errors about architecture

Add platform: linux/amd64 under each service if needed (especially for mailhog).

Q: I see connection refused errors

Make sure to wait ~15 seconds for the database to start. Then reload.

Q: How do I stop the app?

bash
Copy
Edit
docker compose down
📦 This setup includes:
PostgreSQL (port 5433)

MinIO (S3 storage for QR image)

MailHog (Fake email tester, port 8025)

Spring Boot backend (port 8080)

Everything is containerized. No manual install needed.

🙌 Credits
Made with ❤️ by Khoa for the Vietshirt QR project.

vbnet
Copy
Edit

---

Let me know if you'd like:
- To auto-generate this as a `README.md` and commit to your GitHub repo
- A Vietnamese version for your team or partners

Would you like me to export this into a 

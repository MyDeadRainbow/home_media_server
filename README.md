# Home Media Server

This workspace now contains the projects described in `project.md`:
- Spring Boot microservices for media catalog, acquisition, and streaming
- Dockerfiles for each service and docker-compose orchestration
- Vue.js frontend for search, media display, streaming, and closed caption controls

## Project layout

- `backend/`
  - `media-catalog-service` (port 8081)
  - `media-acquisition-service` (port 8082)
  - `media-stream-service` (port 8083)
- `frontend/` (Vite app, exposed on port 5173 via Nginx when containerized)
- `docker-compose.yml`

## Run backend locally

1. Build all services:
   - `cd backend`
   - `mvn clean package`
2. Run each service in separate terminals:
   - `cd backend/media-catalog-service && mvn spring-boot:run`
   - `cd backend/media-acquisition-service && mvn spring-boot:run`
   - `cd backend/media-stream-service && mvn spring-boot:run`

## Run frontend locally

1. `cd frontend`
2. `npm install`
3. `npm run dev`

## Run with Docker Compose

1. Build jars first:
   - `cd backend`
   - `mvn clean package`
2. From repo root:
   - `docker compose up --build`

## API summary

- Catalog service:
  - `GET /api/media?query=...`
  - `POST /api/media`
  - `GET /api/media/{id}`
- Acquisition service:
  - `POST /api/acquisition/import`
- Stream service:
  - `GET /api/stream/{mediaId}/manifest`
  - `GET /api/stream/{mediaId}/captions?lang=en`

## Important note

The torrent lookup and virus scan flow is scaffolded as a safe simulation layer. Replace it with real integrations (torrent indexer client, downloader, scanner engine, and secure file pipeline) before production use.

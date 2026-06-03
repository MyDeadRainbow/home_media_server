# Home Media Server

This workspace now contains the projects described in `project.md`:
- Spring Boot microservices for media catalog, acquisition, streaming, and API gateway
- Dockerfiles for each service and docker-compose orchestration
- Vue.js frontend for search, media display, streaming, and closed caption controls

## Project layout

- `backend/`
  - `shared-lib` (shared Java code used by all backend services)
  - `api-gateway-service` (port 8080)
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
  - `cd backend/api-gateway-service && mvn spring-boot:run`
   - `cd backend/media-catalog-service && mvn spring-boot:run`
   - `cd backend/media-acquisition-service && mvn spring-boot:run`
   - `cd backend/media-stream-service && mvn spring-boot:run`

## Run frontend locally

1. `cd frontend`
2. `npm install`
3. Optional: create `.env` from `.env.example` and set `VITE_API_KEY`
4. `npm run dev`

## Run with Docker Compose

1. Build jars first:
   - `cd backend`
   - `mvn clean package`
2. From repo root:
   - `docker compose up --build`

## API summary

- API gateway:
  - `http://localhost:8080`
  - Requires `X-API-Key` header (or `api_key` query param for media track URLs)
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

## Gateway protection

- `GATEWAY_API_KEY` controls gateway API-key authentication
- `GATEWAY_RATE_LIMIT_PER_MINUTE` controls per-client request limits
- Defaults are set for local development in compose (`dev-local-key`, `240`)

# Parking Service

## Run Application (Gradle)
1. Go to the project root directory.
2. Run:

```bash
./gradlew bootRun
```

3. Open Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Run Application (Docker)
1. Go to the project root directory.
2. Run:

```bash
./gradlew bootJar
docker build -t parking-backend-local -f Dockerfile .
docker run --rm -p 8080:8080 parking-backend-local
```

3. Open Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Run Frontend
1. Go to the project root directory, then `frontend` directory.
2. Run:

```bash
npm run build
docker build -t parking-frontend-local -f Dockerfile .
docker run --rm -p 5173:80 parking-frontend-local
```
3. Open Frontend: `http://localhost:5173`

## Run Both (Frontend + Backend)
1. Go to the project root directory.
2. Run:

```bash
./gradlew bootJar
cd frontend
npm install
npm run build
cd ..
docker-compose up --build
```
3. Open Frontend: `http://localhost:5173`

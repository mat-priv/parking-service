# Parking Service

## Branch Notes
- `master`: version with minimal required features.
- `UI`: version with React UI and additional features.

## Run with Gradle
1. Go to the project root directory.
2. Start the application:

```bash
./gradlew bootRun
```

3. Open Swagger UI:
   http://localhost:8080/swagger-ui/index.html

## Run with Docker
1. Go to the project root directory.
2. Build and run:

```bash
./gradlew bootJar
docker build -t parking-backend-local -f Dockerfile .
docker run --rm -p 8080:8080 parking-backend-local
```

3. Open Swagger UI:
   http://localhost:8080/swagger-ui/index.html

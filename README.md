Run Application (Gradle):
1. Go to the project root directory
2. Run the following command:
   ./gradlew bootRun
3. go to the swagger-ui: http://localhost:8080/swagger-ui/index.html

Run Application (Docker):
1. Go to the project root directory
2. Run the following commands:
./gradlew bootJar
docker build -t parking-backend-local -f Dockerfile .
docker run --rm -p 8080:8080 parking-backend-local
3. go to the swagger-ui: http://localhost:8080/swagger-ui/index.html

Run frontend:
1. Go to the project root directory, then to frontend directory
2. Run the following commands:
npm run build
docker build -t parking-frontend-local -f Dockerfile .
docker run --rm -p 5173:80 parking-frontend-local

Run both (frontend + backend):
1. Go to the project root directory
2. Run the following commands:
./gradlew bootJar
cd frontend
npm install
npm run build
cd ..
docker-compose up --build

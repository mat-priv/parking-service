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

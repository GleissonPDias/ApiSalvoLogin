# 1. Estágio de Build (Compila o código)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build --no-daemon

# 2. Estágio de Execução (Roda o servidor)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copia o arquivo gerado (Ajuste o nome se o seu projeto tiver outro nome)
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
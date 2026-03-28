# 1. Estágio de Build (Compila o código)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew

# Mudança aqui: Usamos shadowJar em vez de build para ser mais direto
RUN ./gradlew shadowJar --no-daemon

# 2. Estágio de Execução (Roda o servidor)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Mudança aqui: Como definimos archiveFileName.set("app.jar") no gradle,
# o caminho exato será este abaixo:
COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8080

# O Render passará a porta pela variável $PORT,
# mas o Java rodará o JAR independente disso.
CMD ["java", "-jar", "app.jar"]
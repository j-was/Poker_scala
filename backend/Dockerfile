FROM sbtscala/scala-sbt:eclipse-temurin-17.0.14_7_1.10.11_3.6.4 AS build
WORKDIR /

# Copy build files first for dependency caching
COPY build.sbt ./
COPY project/ ./project/
RUN sbt update

# Copy source code
COPY src/ ./src/
RUN sbt assembly

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl
RUN addgroup -S poker && adduser -S poker -G poker

# Copy the assembled JAR
COPY --from=build /app/app.jar ./app.jar

USER poker
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

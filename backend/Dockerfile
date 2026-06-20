FROM sbtscala/scala-sbt:eclipse-temurin-21_1.x AS build

WORKDIR /build

COPY . .

# Fix the assembly merge strategy without modifying app files
RUN echo '\n\
    assembly / assemblyMergeStrategy := { \n\
    case "module-info.class" => MergeStrategy.discard \n\
    case x => \n\
    val oldStrategy = (assembly / assemblyMergeStrategy).value \n\
    oldStrategy(x) \n\
    }' >> build.sbt && sbt assembly

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl
RUN addgroup -S poker && adduser -S poker -G poker

WORKDIR /app

COPY --from=build /build/target/scala-3.8.3/scala-poker-assembly-0.1.0-SNAPSHOT.jar ./app.jar

USER poker
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
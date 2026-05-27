FROM maven:3.9-eclipse-temurin-21
WORKDIR /workspace
COPY gameengine/pom.xml gameengine/pom.xml
RUN mvn -f gameengine/pom.xml -B -q dependency:go-offline
COPY gameengine/src gameengine/src
CMD ["mvn", "-f", "gameengine/pom.xml", "-B", "verify"]

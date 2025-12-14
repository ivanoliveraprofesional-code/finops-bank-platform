# -----------------------------------------------------------------------------
# Stage 1: The Runner (JRE 21 Alpine)
# We use a JRE (Java Runtime Environment) instead of JDK to save ~200MB of space.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Security: Run as a non-root user (Standard Big Tech Security Policy)
RUN addgroup -S finops && adduser -S finops -G finops
USER finops:finops

WORKDIR /app

# Copy the built JAR (We assume mvn install ran previously in the pipeline)
# Note: The wildcard *.jar ensures we don't need to hardcode the version
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# -----------------------------------------------------------------------------
# JVM Optimization for Kubernetes (The "FinOps" Secret Sauce)
# -----------------------------------------------------------------------------
# -XX:+UseContainerSupport: Tells JVM it's inside a container (cgroups aware)
# -XX:MaxRAMPercentage=75.0:  Dynamically sets Heap size based on K8s Limits. 
#                             If K8s gives 1GB, Heap gets 750MB. OS gets 250MB.
# -XX:+ExitOnOutOfMemoryError: If it crashes, crash hard so K8s restarts it.
# -----------------------------------------------------------------------------
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "/app/app.jar"]
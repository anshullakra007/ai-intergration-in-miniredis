# ==============================================================================
# MiniRedis Dockerfile
# ==============================================================================

# Use a minimal, secure Alpine Linux based Java 21 image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the project source files into the container
COPY . .

# Compile the Java source code
RUN javac Main.java

# Define the container's entrypoint command
CMD ["java", "Main"]
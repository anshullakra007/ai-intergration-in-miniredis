# Use a lightweight Java image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy all project files (Main.java, etc.) into the container
COPY . .

# Compile the Main.java file
RUN javac Main.java

# Run the compiled Main class
CMD ["java", "Main"]
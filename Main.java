import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Main {
    // 💾 This Map acts as our "Database" (In-Memory)
    private static final Map<String, String> dataStore = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // ☁️ CLOUD DEPLOYMENT LOGIC:
        int port = 6379; 
        String envPort = System.getenv("PORT");
        if (envPort != null) {
            port = Integer.parseInt(envPort);
        }

        System.out.println("🚀 MiniRedis Server starting on port " + port + "...");
        
        // 1. Listen on the dynamic Port
        ServerSocket serverSocket = new ServerSocket(port);
        
        // 2. ThreadPool to handle multiple clients at once
        ExecutorService threadPool = Executors.newCachedThreadPool();

        while (true) {
            // 3. Wait for a client to connect
            Socket clientSocket = serverSocket.accept();
            System.out.println("✅ New Client Connected: " + clientSocket.getInetAddress());
            
            // 4. Handle client in a background thread
            threadPool.submit(() -> handleClient(clientSocket));
        }
    }

    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Read the first line of the request
            String inputLine = in.readLine();
            if (inputLine == null) return;

            // 🔍 HEALTH CHECK: Is this a Browser/HTTP request?
            // If the request contains "HTTP", we assume it's a browser check from Render.
            if (inputLine.contains("HTTP/1.1") || inputLine.contains("HTTP/1.0")) {
                // Send a valid HTTP response so Render knows we are healthy
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println(); // Standard HTTP blank line separation
                out.println("<html><body><h1>&#9889; MiniRedis is Live!</h1><p>The TCP Server is running.</p></body></html>");
                return; // Close connection immediately for browsers
            }

            // ⚡ STANDARD REDIS LOGIC (for real clients)
            // Process the first line we already read
            processCommand(inputLine, out);

            // Process the rest of the lines
            while ((inputLine = in.readLine()) != null) {
                processCommand(inputLine, out);
            }

        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }

    // Helper method to handle the specific Redis commands
    private static void processCommand(String inputLine, PrintWriter out) {
        String[] parts = inputLine.split(" ");
        if (parts.length == 0) return;
        
        String command = parts[0].toUpperCase();

        if (command.equals("SET") && parts.length >= 3) {
            dataStore.put(parts[1], parts[2]);
            out.println("OK");
        } 
        else if (command.equals("GET") && parts.length >= 2) {
            String value = dataStore.getOrDefault(parts[1], "(nil)");
            out.println(value);
        } 
        else if (command.equals("EXIT")) {
            out.println("Bye!");
        } 
        else {
            out.println("ERROR: Unknown Command");
        }
    }
}
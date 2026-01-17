import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Main {
    // 💾 This Map acts as our "Database" (In-Memory)
    private static final Map<String, String> dataStore = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("🚀 MiniRedis Server starting on port 6379...");
        
        // 1. Listen on Port 6379
        ServerSocket serverSocket = new ServerSocket(6379);
        
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
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // PARSE COMMAND: "SET key value" or "GET key"
                String[] parts = inputLine.split(" ");
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
                    break;
                } 
                else {
                    out.println("ERROR: Unknown Command");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }
}
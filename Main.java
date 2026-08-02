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
            String firstLine = in.readLine();
            if (firstLine == null) return;

            // 🔍 HEALTH CHECK: Is this a Browser/HTTP request?
            if (firstLine.contains("HTTP")) {
                // 1. DRAIN HEADERS: Read until the browser is done talking
                // If we don't do this, the Load Balancer thinks we crashed.
                while (in.ready()) {
                    in.read(); 
                }

                // 2. SEND VALID RESPONSE
                String html = getLandingPageHtml();
                byte[] htmlBytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html; charset=utf-8");
                out.println("Content-Length: " + htmlBytes.length);
                out.println("Connection: close");
                out.println(); // Mandatory blank line
                out.println(html);
                out.flush();
                return; // Now we can safely close
            }

            // ⚡ STANDARD REDIS LOGIC (for real clients)
            processCommand(firstLine, out);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processCommand(inputLine, out);
            }

        } catch (IOException e) {
            // Client disconnected normally
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

    private static String getLandingPageHtml() {
        try {
            File file = new File("index.html");
            if (file.exists()) {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            try (InputStream is = Main.class.getResourceAsStream("/index.html")) {
                if (is != null) {
                    return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading index.html: " + e.getMessage());
        }
        return "<html><body><h1>&#9889; MiniRedis is Live!</h1><p>The TCP Server is running.</p></body></html>";
    }
}
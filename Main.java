import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MiniRedis Server Entry Point.
 * 
 * A lightweight, high-performance in-memory key-value storage engine engineered 
 * from scratch using Raw Java Sockets. It mimics core Redis capabilities.
 */
public class Main {
    
    /**
     * The in-memory data store.
     * Uses ConcurrentHashMap to leverage lock-stripping for high concurrency 
     * without severe lock contention.
     */
    private static final Map<String, String> dataStore = new ConcurrentHashMap<>();

    /**
     * The in-memory vector store for embeddings.
     */
    private static final Map<String, double[]> vectorStore = new ConcurrentHashMap<>();

    /**
     * Default TCP port for the MiniRedis server.
     */
    private static final int DEFAULT_PORT = 6379;

    /**
     * The main execution thread for the MiniRedis server.
     * 
     * @param args Command line arguments (not used).
     * @throws IOException If a network error occurs during server startup.
     */
    public static void main(String[] args) throws IOException {
        int port = getPort();

        System.out.println("🚀 MiniRedis Server starting on port " + port + "...");
        
        // 1. Initialize the Server Socket
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            
            // 2. Initialize a Cached Thread Pool to handle variable client load efficiently
            ExecutorService threadPool = Executors.newCachedThreadPool();

            // 3. Enter the main event loop to accept incoming client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ New Client Connected: " + clientSocket.getInetAddress());
                
                // 4. Offload client handling to a background thread to prevent blocking
                threadPool.submit(() -> handleClient(clientSocket));
            }
        }
    }

    /**
     * Resolves the port the server should bind to. 
     * Defaults to 6379 unless the "PORT" environment variable is defined.
     * 
     * @return The integer port number.
     */
    private static int getPort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable. Falling back to default.");
            }
        }
        return DEFAULT_PORT;
    }

    /**
     * Handles an individual client connection.
     * Capable of routing HTTP Health Checks (from load balancers) or 
     * raw TCP Redis commands (from standard clients).
     * 
     * @param socket The client's TCP socket.
     */
    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Read the first line of the incoming request
            String firstLine = in.readLine();
            if (firstLine == null) return;

            // 🔍 PROTOCOL ROUTING: Detect if this is an HTTP request (Browser/Load Balancer)
            if (firstLine.contains("HTTP")) {
                handleHttpRequest(in, out);
                return; 
            }

            // ⚡ STANDARD REDIS PROTOCOL ROUTING
            processCommand(firstLine, out);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processCommand(inputLine, out);
            }

        } catch (IOException e) {
            // Client disconnected normally, or a network disruption occurred.
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    /**
     * Handles HTTP requests by draining headers and serving the landing page.
     * 
     * @param in  The client's input stream reader.
     * @param out The client's output stream writer.
     * @throws IOException If a stream error occurs.
     */
    private static void handleHttpRequest(BufferedReader in, PrintWriter out) throws IOException {
        // 1. DRAIN HEADERS: Read until the HTTP client is done transmitting headers.
        // Failing to drain headers can cause connection resets with strict load balancers.
        while (in.ready()) {
            in.read(); 
        }

        // 2. SEND VALID HTTP 200 RESPONSE
        String html = getLandingPageHtml();
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
        
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + htmlBytes.length);
        out.println("Connection: close");
        out.println(); // Mandatory blank line separates headers from body
        out.println(html);
        out.flush();
    }

    /**
     * Parses and processes a raw Redis command, mutating the datastore or fetching data.
     * 
     * @param inputLine The raw command string received from the client.
     * @param out       The client's output stream writer.
     */
    private static void processCommand(String inputLine, PrintWriter out) {
        String[] parts = inputLine.split(" ");
        if (parts.length == 0) return;
        
        String command = parts[0].toUpperCase();

        switch (command) {
            case "SET":
                if (parts.length >= 3) {
                    dataStore.put(parts[1], parts[2]);
                    out.println("OK");
                } else {
                    out.println("ERROR: SET requires a key and value");
                }
                break;
            case "GET":
                if (parts.length >= 2) {
                    String value = dataStore.getOrDefault(parts[1], "(nil)");
                    out.println(value);
                } else {
                    out.println("ERROR: GET requires a key");
                }
                break;
            case "DEL":
                if (parts.length >= 2) {
                    dataStore.remove(parts[1]);
                    out.println("1"); // Indicate 1 key was deleted
                } else {
                    out.println("ERROR: DEL requires a key");
                }
                break;
            case "PING":
                out.println("PONG");
                break;
            case "VSET":
                if (parts.length >= 3) {
                    try {
                        String[] vals = parts[2].split(",");
                        double[] vector = new double[vals.length];
                        for (int i = 0; i < vals.length; i++) {
                            vector[i] = Double.parseDouble(vals[i]);
                        }
                        vectorStore.put(parts[1], vector);
                        out.println("OK");
                    } catch (Exception e) {
                        out.println("ERROR: Invalid vector format");
                    }
                } else {
                    out.println("ERROR: VSET requires a key and a comma-separated list of floats");
                }
                break;
            case "VSIMILAR":
                if (parts.length >= 3) {
                    try {
                        String[] vals = parts[1].split(",");
                        double[] target = new double[vals.length];
                        for (int i = 0; i < vals.length; i++) {
                            target[i] = Double.parseDouble(vals[i]);
                        }
                        int topK = Integer.parseInt(parts[2]);
                        
                        java.util.List<java.util.Map.Entry<String, Double>> results = new java.util.ArrayList<>();
                        for (Map.Entry<String, double[]> entry : vectorStore.entrySet()) {
                            double sim = cosineSimilarity(target, entry.getValue());
                            results.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), sim));
                        }
                        results.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                        
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < Math.min(topK, results.size()); i++) {
                            sb.append(results.get(i).getKey()).append(":").append(results.get(i).getValue());
                            if (i < Math.min(topK, results.size()) - 1) sb.append(", ");
                        }
                        if (sb.length() == 0) out.println("(empty)");
                        else out.println(sb.toString());
                    } catch (Exception e) {
                        out.println("ERROR: Invalid vector or top_k format");
                    }
                } else {
                    out.println("ERROR: VSIMILAR requires a comma-separated list of floats and top_k");
                }
                break;
            case "ASK_AI":
                if (parts.length >= 2) {
                    String prompt = inputLine.substring(7).trim();
                    try {
                        double[] promptEmbedding = GeminiClient.getEmbedding(prompt);
                        
                        String bestMatchKey = null;
                        double highestSim = -1.0;
                        for (Map.Entry<String, double[]> entry : vectorStore.entrySet()) {
                            double sim = cosineSimilarity(promptEmbedding, entry.getValue());
                            if (sim > highestSim) {
                                highestSim = sim;
                                bestMatchKey = entry.getKey();
                            }
                        }
                        
                        if (highestSim > 0.90 && bestMatchKey != null && dataStore.containsKey(bestMatchKey)) {
                            System.out.println("Cache Hit! Similarity: " + highestSim);
                            out.println(dataStore.get(bestMatchKey));
                        } else {
                            System.out.println("Cache Miss! Calling Gemini...");
                            String answer = GeminiClient.generateAnswer(prompt);
                            
                            String newKey = "prompt:" + System.currentTimeMillis();
                            vectorStore.put(newKey, promptEmbedding);
                            dataStore.put(newKey, answer);
                            
                            out.println(answer);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        out.println("ERROR: Failed to process AI request: " + e.getMessage());
                    }
                } else {
                    out.println("ERROR: ASK_AI requires a prompt");
                }
                break;
            case "EXIT":
            case "QUIT":
                out.println("Bye!");
                break;
            default:
                out.println("ERROR: Unknown Command");
                break;
        }
    }

    /**
     * Safely loads the index.html landing page from the filesystem or classpath.
     * 
     * @return The HTML content as a String.
     */
    private static String getLandingPageHtml() {
        try {
            // Attempt to read from the local file system first (useful for dev)
            File file = new File("index.html");
            if (file.exists()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                return new String(bytes, StandardCharsets.UTF_8);
            }
            
            // Fallback to reading from classpath resources (useful when packaged as JAR)
            try (InputStream is = Main.class.getResourceAsStream("/index.html")) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading index.html: " + e.getMessage());
        }
        // Ultimate fallback if no file is found
        return "<html><body><h1>&#9889; MiniRedis is Live!</h1><p>The TCP Server is running.</p></body></html>";
    }

    /**
     * Computes the cosine similarity between two vectors.
     * 
     * @param v1 First vector
     * @param v2 Second vector
     * @return The cosine similarity score
     */
    private static double cosineSimilarity(double[] v1, double[] v2) {
        if (v1.length != v2.length) return 0.0;
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
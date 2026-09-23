import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiClient {
    private static final String API_KEY = System.getenv("GEMINI_API_KEY");
    private static final HttpClient client = HttpClient.newHttpClient();

    public static String generateAnswer(String prompt) throws Exception {
        if (API_KEY == null) {
            System.out.println("WARN: GEMINI_API_KEY not set. Returning mock response.");
            return "Mock response for: " + prompt;
        }
        
        String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String jsonBody = "{" +
                "\"contents\": [{\"parts\":[{\"text\": \"" + escapedPrompt + "\"}]}]" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String resBody = response.body();
        
        // Very basic string parsing to avoid external dependencies
        String target = "\"text\": \"";
        int start = resBody.indexOf(target);
        if (start == -1) {
            throw new RuntimeException("Failed to parse Gemini response: " + resBody);
        }
        start += target.length();
        
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < resBody.length(); i++) {
            char c = resBody.charAt(i);
            if (c == '\\' && i + 1 < resBody.length()) {
                char next = resBody.charAt(i + 1);
                if (next == 'n') {
                    sb.append("\n");
                    i++;
                } else if (next == '"') {
                    sb.append("\"");
                    i++;
                } else if (next == '\\') {
                    sb.append("\\");
                    i++;
                } else {
                    sb.append(c);
                }
            } else if (c == '"') {
                break; // End of text
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static double[] getEmbedding(String text) throws Exception {
        if (API_KEY == null) {
            System.out.println("WARN: GEMINI_API_KEY not set. Returning mock embedding.");
            // Return a dummy deterministic embedding based on string hash to test similarity
            double[] mock = new double[768];
            int hash = text.hashCode();
            for(int i = 0; i < 768; i++) {
                mock[i] = (hash % (i + 1)) / (double)(i + 1);
            }
            return mock;
        }
        
        String escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String jsonBody = "{" +
                "\"model\": \"models/text-embedding-004\"," +
                "\"content\": {\"parts\":[{\"text\": \"" + escapedText + "\"}]}" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String resBody = response.body();
        
        String target = "\"values\": [";
        int start = resBody.indexOf(target);
        if (start == -1) {
            throw new RuntimeException("Failed to parse embedding response: " + resBody);
        }
        start += target.length();
        int end = resBody.indexOf("]", start);
        if (end == -1) {
            throw new RuntimeException("Failed to parse embedding response (no end bracket)");
        }
        
        String valuesStr = resBody.substring(start, end).trim();
        String[] parts = valuesStr.split(",");
        double[] embedding = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            embedding[i] = Double.parseDouble(parts[i].trim());
        }
        return embedding;
    }

    public static void main(String[] args) {
        if (System.getenv("GEMINI_API_KEY") == null) {
            System.err.println("Please set GEMINI_API_KEY environment variable to test.");
            return;
        }
        try {
            System.out.println("Testing getEmbedding...");
            double[] emb = getEmbedding("Hello world");
            System.out.println("Embedding size: " + emb.length);
            System.out.println("First few values: " + emb[0] + ", " + emb[1] + ", " + emb[2]);
            
            System.out.println("\nTesting generateAnswer...");
            String answer = generateAnswer("What is the capital of France?");
            System.out.println("Answer: " + answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

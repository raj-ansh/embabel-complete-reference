package com.northwind.raw;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Calls an OpenAI-compatible chat endpoint using only the JDK. Teaching example — not production code. */
public class RawChatCall {

    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv().getOrDefault("LLM_BASE_URL", "https://api.openai.com/v1");
        String apiKey = System.getenv("LLM_API_KEY");          // never hard-code keys
        String model = System.getenv("LLM_MODEL");             // choose a current model from your provider

        String body = """
            {
              "model": "%s",
              "temperature": 0.2,
              "messages": [
                {"role": "system", "content": "Reply in one short sentence."},
                {"role": "user", "content": "What is a context window?"}
              ]
            }""".formatted(model);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
        System.out.println(response.body());                  // raw JSON — still needs parsing
    }
}

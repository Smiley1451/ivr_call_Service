package com.labourconnect.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final OkHttpClient httpClient;
    private final Gson gson;

    public GroqService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public double[] getCoordinates(String location) {
        log.info("Getting coordinates for location: {}", location);
        
        if (location == null || location.trim().isEmpty()) {
            return new double[]{0.0, 0.0};
        }

        String prompt = String.format(
                "Extract latitude and longitude for the location '%s'. " +
                "Return ONLY a JSON object with 'lat' and 'lng' fields. " +
                "Example: {\"lat\": 12.9716, \"lng\": 77.5946}. " +
                "If location is unknown or invalid, return {\"lat\": 0.0, \"lng\": 0.0}.",
                location
        );

        try {
            String response = callGroqApi(prompt);
            return parseCoordinates(response);
        } catch (Exception e) {
            log.error("Error getting coordinates from Groq: {}", e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }

    private String callGroqApi(String prompt) throws IOException {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.1);

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    private double[] parseCoordinates(String jsonResponse) {
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String content = responseObj.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            // Extract JSON from content (in case there's extra text)
            int startIndex = content.indexOf("{");
            int endIndex = content.lastIndexOf("}") + 1;
            
            if (startIndex >= 0 && endIndex > startIndex) {
                String jsonStr = content.substring(startIndex, endIndex);
                JsonObject coords = JsonParser.parseString(jsonStr).getAsJsonObject();
                
                double lat = coords.has("lat") ? coords.get("lat").getAsDouble() : 0.0;
                double lng = coords.has("lng") ? coords.get("lng").getAsDouble() : 0.0;
                
                return new double[]{lat, lng};
            }
        } catch (Exception e) {
            log.error("Error parsing Groq response: {}", e.getMessage());
        }
        return new double[]{0.0, 0.0};
    }
}
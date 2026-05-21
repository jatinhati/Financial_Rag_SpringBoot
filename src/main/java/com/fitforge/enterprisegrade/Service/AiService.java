package com.fitforge.enterprisegrade.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating responses using OpenAI-compatible API.
 * Calls the configured model (minimax-m2.5-free) via opencode.ai endpoint.
 */
@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String openaiBaseUrl;
    private final String openaiApiKey;
    private final String openaiModel;

    public AiService(
            @Value("${spring.ai.openai.base-url:https://opencode.ai/zen}") String baseUrl,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:minimax-m2.5-free}") String model) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.openaiBaseUrl = baseUrl;
        this.openaiApiKey = apiKey;
        this.openaiModel = model;
    }

    /**
     * Generate a response from the configured OpenAI-compatible model.
     * Uses the RAG-augmented prompt to provide context-aware answers.
     *
     * @param augmentedPrompt the RAG-augmented prompt to send to the LLM
     * @return the model's response text
     */
    public String generateResponse(String augmentedPrompt) {
        if (augmentedPrompt == null || augmentedPrompt.isBlank()) {
            log.warn("Empty prompt provided to generateResponse");
            return "Error: Empty prompt provided.";
        }

        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openaiModel);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", augmentedPrompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

            // Build headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            if (openaiApiKey != null && !openaiApiKey.isBlank()) {
                headers.put("Authorization", "Bearer " + openaiApiKey);
            }

            // Make API call
            String url = openaiBaseUrl + "/v1/chat/completions";
            log.info("Calling OpenAI API at: {} with model: {}", url, openaiModel);

            // Create a custom RestTemplate with headers
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(requestBody, getHttpHeaders(headers));

            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Extract response content
                if (body.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        if (firstChoice.containsKey("message")) {
                            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                            String content = (String) message.get("content");
                            log.info("Successfully generated response from OpenAI model: {}", openaiModel);
                            return content;
                        }
                    }
                }
            }

            log.warn("Unexpected response structure from OpenAI API");
            return "Error: Could not parse response from model.";
        }
        catch (Exception ex) {
            log.error("Error calling OpenAI API at {}: {}", openaiBaseUrl, ex.getMessage(), ex);
            return "Error: Failed to generate response from model. " + ex.getMessage();
        }
    }

    private org.springframework.http.HttpHeaders getHttpHeaders(Map<String, String> headerMap) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headerMap.forEach(headers::set);
        return headers;
    }
}

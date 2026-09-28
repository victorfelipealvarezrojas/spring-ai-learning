package com.genai.chat.huggingface;


import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class HuggingFaceService {

    private static final String HUGGINGFACE_API_KEY = "HUGGINGFACE_API_KEY";
    private static final String CONTENT_TYPE = "application/json";

    @Value("${spring.ai.huggingface.url}")
    private String huggingFaceUrl;

    @Value("${spring.ai.huggingface.model}")
    private String model;

    @Value("${spring.ai.huggingface.max-new-tokens}")
    private int maxNewTokens;

    @Value("${spring.ai.huggingface.temperature}")
    private double temperature;

    private final ObjectMapper objectMapper;

    public HuggingFaceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String chat(String prompt) throws HuggingFaceChatException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var request = getRequest(prompt);
            var response = httpClient.execute(request, resp -> {
                String body = EntityUtils.toString(resp.getEntity());
                if (resp.getCode() >= 300) {
                    throw new IOException("HuggingFace devolvió HTTP " + resp.getCode() + ": " + body);
                }
                return body;
            });
            return parseResponse(response);
        } catch (IOException ioe) {
            throw new HuggingFaceChatException("Error: ", ioe);
        }
    }

    private String parseResponse(String response) {
        System.out.println("RESPUESTA CRUDA HF: " + response);
        List<Map<String, Object>> hfResponse = objectMapper.readValue(response, List.class);
        if (hfResponse.isEmpty()) {
            throw new HuggingFaceChatException("HuggingFace no devolvió resultados: " + response);
        }
        Object summary = hfResponse.get(0).get("summary_text");
        if (summary == null) {
            throw new HuggingFaceChatException("Respuesta de HuggingFace sin 'summary_text': " + response);
        }
        return summary.toString();
    }

    private HttpPost getRequest(String prompt) {
        var request = new HttpPost(huggingFaceUrl + "/hf-inference/models/" + model);
        var apiKey = System.getenv(HUGGINGFACE_API_KEY);
        request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        Map<String, Object> parameters = Map.of(
                "max_new_tokens", maxNewTokens,
                "temperature", temperature
        );

        Map<String, Object> options = Map.of(
                "wait_for_model", true
        );

        Map<String, Object> body = Map.of(
                "inputs", prompt,
                "parameters", parameters,
                "options", options
        );

        String requestBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(requestBody));
        return request;
    }
}
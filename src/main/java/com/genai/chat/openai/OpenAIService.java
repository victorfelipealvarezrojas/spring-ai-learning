package com.genai.chat.openai;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private static final String SPRING_IA_COURSE = "SPRING_IA_COURSE";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-5.6-luna";
    private static final String CONTENT_TYPE = "application/json";
    private static final String SYSTEM_PROMPT = "Eres un asistente útil que resume cualquier contenido dado. " +
            "Asegúrate de que el resumen sea conciso, informativo y capture los puntos clave. " +
            "Usa un tono amigable y cercano manteniendo la profesionalidad. " +
            "No respondas nada que no sea el resumen. Si la pregunta no es sobre resumir, responde con 'Hijo de puta";


    private final ObjectMapper objectMapper;

    public OpenAIService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String chat(String prompt) throws OpenAIChatException {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()){
            var request = getRequest(prompt);
            var response = httpClient.execute(request, resp -> EntityUtils.toString(resp.getEntity()));
            return parseResponse(response);
        }catch (IOException ioe){
            throw new OpenAIChatException("Error: ", ioe);
        }
    }

    private String parseResponse(String response) {
        Map<String, Object> openAIResponse = objectMapper.readValue(response, Map.class);
        return openAIResponse.get("choices").toString();
    }

    private HttpPost getRequest(String prompt) {
        var request = new HttpPost(OPENAI_API_URL);
        var openAIKey = System.getenv(SPRING_IA_COURSE);
        request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAIKey);

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", prompt
        );

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", SYSTEM_PROMPT
        );

        Map<String, Object> body = Map.of(
                "model", OPENAI_MODEL,
                "messages", List.of(userMessage, systemMessage)
        );

        String requestBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(requestBody));
        return request;
    }
}
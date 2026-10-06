package com.genai.chat.openai;


import com.genai.chat.openai.dto.response.SummarizationResponse;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.awt.*;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("api/openapi/chat")
public class OpenAiChatController {

    private final static String SYSTEM_PROMPT = "Eres un asistente útil que resume cualquier contenido dado. " +
            "Asegúrate de que el resumen sea conciso, informativo y capture los puntos clave. " +
            "Usa un tono amigable y cercano manteniendo la profesionalidad. " +
            "No respondas nada que no sea el resumen. Si la pregunta no es sobre resumir, " +
            "responde con 'Solo puedo ayudar con tareas de resumen' y le agregas unos cuantos garabatos " +
            "chilenos partiendo por el siguiente: maldito hijo de perra!. ";

    private final ChatClient chatClient;
    private final OpenAIService openAIService;

    // spring container instance to beans @Qualifier("beans_name")
    public OpenAiChatController(@Qualifier("openAiChatClientWithMemory") ChatClient chatClient,
                                OpenAIService openAIService
    ) {
        this.chatClient = chatClient;
        this.openAIService = openAIService;
    }

    @PostMapping("/summarize")
    public ChatClientResponse summarize(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .chatClientResponse();
    }

    @PostMapping("/summarize-meeting-notes")
    public String summarizeMeetingNotes(@RequestBody String meetingNotest) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("¿Puedes resumir las siguientes notas de la reunión: {meetingNotes}" +
                                " Usa el formato descrito en el siguiente ejemplo mientras lo haces." +
                                " Entrada: En la reunión de estrategia de ventas de hoy, revisamos los objetivos del Q3 y las brechas de desempeño. El equipo acordó enfocarse en clientes enterprise." +
                                " Se propuso expandirse hacia dos nuevas regiones. Marketing sugirió alinear las campañas con los objetivos de ventas para mejorar." +
                                " Salida:" +
                                " Action Items:" +
                                "* Enfocarse en clientes enterprise y partnerships." +
                                "* Explorar la expansión hacia dos nuevas regiones." +
                                "* Alinear las campañas de marketing con los objetivos de ventas." +
                                " Decisions:" +
                                "* Clientes enterprise priorizados para el Q3." +
                                "* Marketing y ventas trabajarán en conjunto en la conversión de leads.")
                        .param("meetingNotes", meetingNotest))
                .call()
                .content();
    }

    @PostMapping("/summarize-meeting-notes-structured")
    public List<SummarizationResponse> summarizeMeetingNotesStructuredOutput(@RequestBody String meetingNotest) {
        try {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("¿Puedes resumir las siguientes notas de la reunión: {meetingNotes}" +
                                    " Dame tres resultados diferentes, tres resúmenes diferentes en el mismo formato, para que pueda elegir." +
                                    " Usa el formato descrito en el siguiente ejemplo mientras lo haces." +
                                    " Entrada: En la reunión de estrategia de ventas de hoy, revisamos los objetivos del Q3 y las brechas de desempeño. El equipo acordó enfocarse en clientes enterprise." +
                                    " Se propuso expandirse hacia dos nuevas regiones. Marketing sugirió alinear las campañas con los objetivos de ventas para mejorar." +
                                    " Salida:" +
                                    " Action Items:" +
                                    "* Enfocarse en clientes enterprise y partnerships." +
                                    "* Explorar la expansión hacia dos nuevas regiones." +
                                    "* Alinear las campañas de marketing con los objetivos de ventas." +
                                    " Decisions:" +
                                    "* Clientes enterprise priorizados para el Q3." +
                                    "* Marketing y ventas trabajarán en conjunto en la conversión de leads.")
                            .param("meetingNotes", meetingNotest))
                    .call()
                    .entity(new ParameterizedTypeReference<List<SummarizationResponse>>() {
                    });
        } catch (Exception e) {
            return Collections.singletonList(new SummarizationResponse(null, null, e.getMessage()));
        }
    }

    @PostMapping("/summarize-meeting-notes-structured-list")
    public SummarizationResponse summarizeMeetingNotesStructuredOutputList(@RequestBody String meetingNotest) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("¿Puedes resumir las siguientes notas de la reunión: {meetingNotes}" +
                                " Usa el formato descrito en el siguiente ejemplo mientras lo haces." +
                                " Entrada: En la reunión de estrategia de ventas de hoy, revisamos los objetivos del Q3 y las brechas de desempeño. El equipo acordó enfocarse en clientes enterprise." +
                                " Se propuso expandirse hacia dos nuevas regiones. Marketing sugirió alinear las campañas con los objetivos de ventas para mejorar." +
                                " Salida:" +
                                " Action Items:" +
                                "* Enfocarse en clientes enterprise y partnerships." +
                                "* Explorar la expansión hacia dos nuevas regiones." +
                                "* Alinear las campañas de marketing con los objetivos de ventas." +
                                " Decisions:" +
                                "* Clientes enterprise priorizados para el Q3." +
                                "* Marketing y ventas trabajarán en conjunto en la conversión de leads.")
                        .param("meetingNotes", meetingNotest))
                .call()
                .entity(SummarizationResponse.class);
    }



    @PostMapping("/summarize-open-ai-java-client")
    public String summarizeOpenAIJavaClient(@RequestBody String prompt) {
        return this.openAIService.chat(prompt);
    }

    @PostMapping(value = "/summarize-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> summarizeStream(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .stream()
                .content()
                .bufferTimeout(40, Duration.ofMillis(200))
                .map(tokenList -> String.join(",", tokenList));
    }

    @PostMapping("/general-chat")
    public String generalChat(@RequestBody String message) {

        var options = OpenAiChatOptions.builder().maxTokens(500).temperature(0.2).topP(0.1);

        return chatClient.prompt()
                .options(options)
                .user(message)
                .call()
                .content();
    }
}

package com.genai.rag.inprompcontext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/it-support")
public class ITSupportController {

    private final ChatClient chatClient;

    private final String SYSTEM_PROMPT = "Eres un asistente de soporte técnico que ayuda a los empleados con problemas " +
            "relacionados con la tecnología. Responde a las preguntas de manera precisa y concisa. Si no conoces la " +
            "respuesta, di que no lo sabes y que aún no conoces ese problema.";

    // contexto estatico
    private final String IN_PROMPT_CONTEXT = "cuando respondas al usuario utiliza el siguente contexto para proporcionar respuestas relevantes: " +
            "Acceso a internet (wi-fi): " +
            "SSID: companyNet" +
            "Contraseña: companyPass123" +
            "Política de seguridad: " +
            "Todos los empleados deben cambiar sus contraseñas cada 90 días y utilizar autenticación de dos factores para acceder a los sistemas internos." +
            "Soporte de hardware: " +
            "Para problemas con el hardware, los empleados pueden comunicarse con el equipo de soporte técnico a través del correo electrónico " +
            "el cual es: soporte@sopoerte.cl  o al fono: +56 944905069 " +
            "{message}";

    public ITSupportController(@Qualifier("openAiChatClientWithMemory") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/ask")
    public String getItSupport(@RequestBody String message){
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text(IN_PROMPT_CONTEXT).param("message",message))
                .call()
                .content();

    }


}

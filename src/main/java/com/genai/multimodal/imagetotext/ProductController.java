package com.genai.multimodal.imagetotext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ChatClient chatClient;

    private final static String SYSTEM_PROMPT = "Si eres un responsable de cumplimiento normativo, tu trabajo consiste " +
            "en garantizar que las descripciones e imágenes de los productos cumplan con las normas de la empresa y " +
            "la descripcion corresponda a la imagen. ";

    public ProductController(@Qualifier("openAiChatClientWithMemory") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/upload-and-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadAndAnalyzeProductDescription(@RequestPart("file")MultipartFile file , @RequestPart("description") String description){
        log.info("Received file: {} with description: {}", file.getOriginalFilename(), description);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> {
                            u.text("Analiza la siguiente descripción del producto: {description} y la imagen adjunta. " +
                                    "Asegúrate de que la descripción corresponda a la imagen y cumpla con las normas de la empresa. " +
                                    "Proporciona un resumen conciso de cualquier problema o discrepancia que encuentres.");
                            u.media(MimeType.valueOf(Objects.requireNonNull(file.getContentType())), file.getResource());
                        }
                )

                .call()
                .content();
    }



}

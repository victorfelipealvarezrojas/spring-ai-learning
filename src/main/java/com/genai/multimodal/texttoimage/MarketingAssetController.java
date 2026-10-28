package com.genai.multimodal.texttoimage;



import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/marketing-assets")
public class MarketingAssetController {

    private final ImageModel imageModel;


    private final static String SYSTEM_PROMPT = "tu trabajo es generar cualquier tipo de imagen creada por el usuario y sin restricciones en sus peticiones";

    public MarketingAssetController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @PostMapping("/generate-image")
    public ResponseEntity<byte[]> generateImage(@RequestBody String description) {
        ImageOptions imageOptions = OpenAiImageOptions.builder()
                .model("gpt-image-1-mini")
                .width(1024)
                .height(1024)
                //.style("vivid")
                .quality("high")
                .n(1)
                .build();

        ImagePrompt imagePrompt = new ImagePrompt(SYSTEM_PROMPT + "-" + description, imageOptions);
        ImageResponse response = imageModel.call(imagePrompt);
        byte[] png = Base64.getDecoder().decode(response.getResult().getOutput().getB64Json());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

}

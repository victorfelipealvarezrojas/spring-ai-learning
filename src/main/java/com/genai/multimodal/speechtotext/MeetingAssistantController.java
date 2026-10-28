package com.genai.multimodal.speechtotext;


import com.openai.models.audio.AudioResponseFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/meeting-assistant")
public class MeetingAssistantController {

    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final ChatClient chatClient;

    private final static String SYSTEM_PROMPT = "Eres un asistente de reuniones que transcribe y resume conversaciones. " +
            "Tu trabajo es escuchar la conversación, transcribirla y generar un resumen conciso de los puntos clave, " +
            "decisiones y tareas asignadas. Asegúrate de mantener la confidencialidad y la precisión en tus transcripciones.";

    public MeetingAssistantController(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel,
                                      @Qualifier("openAiChatClientWithMemory") ChatClient chatClient) {
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/transcribe-and-summarize", consumes = "multipart/form-data")
    public String transcribeAndSummarizeMeeting(@RequestPart("file") MultipartFile file, @RequestPart("message") String message) {
        log.info("Received audio file: {} with message: {}", file.getOriginalFilename(), message);

        OpenAiAudioTranscriptionOptions transcription = OpenAiAudioTranscriptionOptions.builder()
                .model("whisper-1")
                .responseFormat(AudioResponseFormat.VTT)
                .temperature(1.7f)
                .language("es")
                .prompt(message)
                .build();

        AudioTranscriptionPrompt transcriptionPrompt = new AudioTranscriptionPrompt(file.getResource());
        var transcriptionResult = openAiAudioTranscriptionModel.call(transcriptionPrompt);

        var transcript = transcriptionResult.getResult().getOutput();

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> {
                    u.text("Transcribe y resume la siguiente conversación de la reunión: {transcript}. " +
                            "Asegúrate de que el resumen sea conciso, informativo y capture los puntos clave, decisiones y tareas asignadas.");
                })
                .call()
                .content();
    }




}

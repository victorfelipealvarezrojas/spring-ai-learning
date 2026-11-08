package com.genai;

import com.genai.rag.service.RagIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GeanaiApplication implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;

    public GeanaiApplication(RagIngestionService ragIngestionService) {
        this.ragIngestionService = ragIngestionService;
    }

    public static void main(String[] args) {
		SpringApplication.run(GeanaiApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
        ragIngestionService.initializePgVectorStore();
    }
}

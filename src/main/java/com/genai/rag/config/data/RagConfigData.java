package com.genai.rag.config.data;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.rag")
public class RagConfigData {
    private boolean forceRebuild;
    private int topK;
    private double similarityThreshold;
    private ChunkProperties chunk;

    @Data
    public static class ChunkProperties {
        private int size;
        private int minChunkSize;
        private int minChunkEmbed;
        private int maxNumChunks;
        private boolean keepSeparator;
    }

}

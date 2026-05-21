package com.fitforge.enterprisegrade.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ai.embedding.EmbeddingModel;

@Configuration
public class EmbeddingModelSelectorConfig {

    @Primary
    @Bean
    public EmbeddingModel primaryEmbeddingModel(
            @Qualifier("ollamaEmbeddingModel") ObjectProvider<EmbeddingModel> ollamaProvider,
            @Qualifier("openAiEmbeddingModel") ObjectProvider<EmbeddingModel> openAiProvider,
            @Value("${app.ai.embedding-provider:ollama}") String provider
    ) {
        String p = provider == null ? "ollama" : provider.toLowerCase();
        if ("openai".equals(p)) {
            EmbeddingModel m = openAiProvider.getIfAvailable();
            if (m != null) {
                return m;
            }
        }

        // prefer ollama by default
        EmbeddingModel m = ollamaProvider.getIfAvailable();
        if (m != null) {
            return m;
        }

        // fallback to openai if available
        m = openAiProvider.getIfAvailable();
        if (m != null) {
            return m;
        }

        throw new IllegalStateException("No EmbeddingModel beans available (expected ollama or openai)");
    }
}


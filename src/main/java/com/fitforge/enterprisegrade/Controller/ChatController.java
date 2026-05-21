package com.fitforge.enterprisegrade.Controller;

import com.fitforge.enterprisegrade.service.AiService;
import com.fitforge.enterprisegrade.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for chat endpoints.
 * Supports RAG-based question answering using vector store retrieval.
 */
@RestController
public class ChatController {

    private final RagService ragService;
    private final AiService aiService;

    public ChatController(RagService ragService, AiService aiService) {
        this.ragService = ragService;
        this.aiService = aiService;
    }

    /**
     * Ask a question and get an answer using RAG.
     * @param question the user's question (defaults to example if not provided)
     * @param topK number of documents to retrieve (default 5)
     * @return the answer augmented by retrieved context
     */
    @GetMapping("/chat")
    public String chat(
            @RequestParam(defaultValue = "How did the Federal Reserve's recent interest rate cut impact various asset classes according to the analysis") String question,
            @RequestParam(defaultValue = "5") int topK) {

        // Build augmented prompt using RAG
        String augmentedPrompt = ragService.ragQuery(question, topK);

        // Generate response using LLM
        String response = aiService.generateResponse(augmentedPrompt);

        return response;
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public String health() {
        return "Chat service is running.";
    }

}

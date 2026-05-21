package com.fitforge.enterprisegrade.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Retrieval-Augmented Generation (RAG).
 * Retrieves relevant documents from the vector store and builds augmented prompts.
 */
@Service
public class RagService {
    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int DEFAULT_RETRIEVAL_COUNT = 5;
    private static final String CONTEXT_SEPARATOR = "\n---\n";

    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Retrieve relevant documents from the vector store for a given query.
     * @param query the search query
     * @param topK number of top results to retrieve (default 5)
     * @return list of relevant documents
     */
    public List<Document> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            log.warn("Empty query provided to retrieve");
            return List.of();
        }

        // For Spring AI 2.0.0-M6, use the string-based similarity search
        List<Document> results = vectorStore.similaritySearch(query);

        // Limit to topK results
        List<Document> limited = results.stream()
                .limit(topK)
                .toList();

        log.info("Retrieved {} documents (limited to {}) for query: {}",
                results.size(), topK, query);
        return limited;
    }

    /**
     * Retrieve relevant documents with default top-K.
     */
    public List<Document> retrieve(String query) {
        return retrieve(query, DEFAULT_RETRIEVAL_COUNT);
    }

    /**
     * Build an augmented prompt by combining the user query with retrieved context.
     * @param userQuery the original user query
     * @param documents the retrieved context documents
     * @return augmented prompt ready for LLM
     */
    public String buildAugmentedPrompt(String userQuery, List<Document> documents) {
        if (documents.isEmpty()) {
            return userQuery + "\n\n(No relevant context available in the knowledge base.)";
        }

        String context = documents.stream()
                .map(doc -> {
                    String source = doc.getMetadata() != null ?
                            (String) doc.getMetadata().getOrDefault("source", "unknown") : "unknown";
                    return "Source: " + source + "\n" + doc.getText();
                })
                .collect(Collectors.joining(CONTEXT_SEPARATOR));

        return String.format(
                "You are a helpful assistant with access to a knowledge base. "
                + "Use the following context to answer the user's question. "
                + "If the context doesn't contain relevant information, say so.\n\n"
                + "Context:\n%s\n\n"
                + "User Question: %s",
                context,
                userQuery
        );
    }

    /**
     * Perform RAG: retrieve context and build an augmented prompt in one call.
     * @param userQuery the user's question
     * @return augmented prompt
     */
    public String ragQuery(String userQuery) {
        List<Document> retrieved = retrieve(userQuery);
        return buildAugmentedPrompt(userQuery, retrieved);
    }

    /**
     * Perform RAG with custom top-K.
     */
    public String ragQuery(String userQuery, int topK) {
        List<Document> retrieved = retrieve(userQuery, topK);
        return buildAugmentedPrompt(userQuery, retrieved);
    }
}

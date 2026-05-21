package com.fitforge.enterprisegrade.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 public class IngestionService implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    // Conservative defaults so embedding requests stay below common context limits.
    private static final int MAX_CHUNK_CHARS = 1200;
    private static final int CHUNK_OVERLAP_CHARS = 120;
    private static final int VECTORSTORE_BATCH_SIZE = 64;

    private final VectorStore vectorStore;

    @Value("classpath:/docs/article_thebeatoct2024.pdf")
    private Resource marketPDF;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        log.info("Starting ingestion service");
        try {
            if (marketPDF == null || !marketPDF.exists()) {
                log.warn("Market PDF resource not found: classpath:/docs/article_thebeatoct2024.pdf");
                return;
            }

            log.info("Found market PDF at {}", marketPDF.getURI());
            ParagraphPdfDocumentReader reader = new ParagraphPdfDocumentReader(marketPDF);
            List<Document> extractedDocuments = reader.get().stream().toList();

            if (extractedDocuments.isEmpty()) {
                log.warn("No paragraphs extracted from PDF: {}", marketPDF.getFilename());
                return;
            }

            String sourceFilename = marketPDF.getFilename() != null ? marketPDF.getFilename() : "unknown.pdf";
            List<Document> documentsToStore = new ArrayList<>();

            for (int paragraphIndex = 0; paragraphIndex < extractedDocuments.size(); paragraphIndex++) {
                Document extracted = extractedDocuments.get(paragraphIndex);
                String text = extracted.getText();
                if (text == null || text.isBlank()) {
                    continue;
                }

                List<String> chunks = splitIntoChunks(text, MAX_CHUNK_CHARS, CHUNK_OVERLAP_CHARS);
                for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                    String chunk = chunks.get(chunkIndex);
                    if (chunk.isBlank()) {
                        continue;
                    }

                    Map<String, Object> metadata = new HashMap<>(extracted.getMetadata());
                    metadata.put("source", sourceFilename);
                    metadata.put("paragraphIndex", paragraphIndex);
                    metadata.put("chunkIndex", chunkIndex);
                    metadata.put("chunkCount", chunks.size());

                    documentsToStore.add(new Document(chunk, metadata));
                }
            }

            if (documentsToStore.isEmpty()) {
                log.warn("No non-empty chunks available for ingestion from {}", sourceFilename);
                return;
            }

            for (int start = 0; start < documentsToStore.size(); start += VECTORSTORE_BATCH_SIZE) {
                int end = Math.min(start + VECTORSTORE_BATCH_SIZE, documentsToStore.size());
                vectorStore.add(documentsToStore.subList(start, end));
            }

            log.info("Ingested {} chunks (from {} extracted paragraphs) into vector store",
                    documentsToStore.size(), extractedDocuments.size());
        }
        catch (Exception ex) {
            log.error("Error during ingestion service startup", ex);
        }
    }

    private static List<String> splitIntoChunks(String text, int maxChars, int overlapChars) {
        List<String> chunks = new ArrayList<>();
        String normalized = text.replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChars, normalized.length());

            // Prefer splitting at whitespace for readability/semantic continuity.
            if (end < normalized.length()) {
                int lastSpace = normalized.lastIndexOf(' ', end);
                if (lastSpace > start + (maxChars / 2)) {
                    end = lastSpace;
                }
            }

            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= normalized.length()) {
                break;
            }

            start = Math.max(end - overlapChars, start + 1);
        }

        return chunks;
    }
}

# Financial RAG Spring Boot

Production-style Retrieval-Augmented Generation (RAG) demo built with Spring Boot 4, Spring AI, and pgvector. It ingests a financial market PDF on startup, stores embeddings in PostgreSQL/pgvector, and serves a simple chat endpoint that answers questions using retrieved context.

## Features

- **RAG pipeline**: PDF ingestion → chunking → embeddings → pgvector storage.
- **Spring AI integration** for embeddings and model calls.
- **OpenAI-compatible chat** via configurable base URL (default: opencode.ai).
- **Embedding provider selection** between OpenAI and Ollama.
- **Docker Compose** setup for local pgvector.

## Architecture Overview

1. **IngestionService** loads `classpath:/docs/article_thebeatoct2024.pdf` on startup.
2. Content is chunked and embedded, then stored in **pgvector**.
3. **RagService** retrieves the top-K matches for a user query.
4. **AiService** sends the augmented prompt to an OpenAI-compatible API.
5. **ChatController** exposes the `/chat` endpoint.

## Requirements

- **Java 25** (set in `pom.xml`)
- **Docker + Docker Compose** (for pgvector)
- **Maven Wrapper** (`./mvnw` or `sh ./mvnw`)

## Quick Start

1. **Start pgvector**
   ```bash
   docker compose up -d
   ```

2. **Set environment variables (if required by your provider)**
   ```bash
   export OPENAI_API_KEY=your_key_here
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```
   If the wrapper is not executable:
   ```bash
   sh ./mvnw spring-boot:run
   ```

4. **Ask a question**
   ```bash
   curl "http://localhost:8080/chat?question=What%20did%20the%20report%20say%20about%20rate%20cuts%3F&topK=5"
   ```

## Configuration

Configuration lives in `src/main/resources/application.yaml`.

### Key settings

- **Vector store**
  - `spring.ai.vectorstore.pgvector.table-name` (default: `market_vectors`)
  - `spring.ai.vectorstore.pgvector.dimensions` (default: `1024`)
  - `spring.datasource.*` for PostgreSQL connectivity

- **OpenAI-compatible provider**
  - `spring.ai.openai.base-url` (default: `https://opencode.ai/zen`)
  - `spring.ai.openai.api-key` (from `OPENAI_API_KEY`)
  - `spring.ai.openai.chat.options.model` (default: `minimax-m2.5-free`)
  - `spring.ai.openai.embedding.options.model` (default: `text-embedding-3-small`)

- **Ollama provider**
  - `spring.ai.ollama.base-url` (default: `http://localhost:11434`)
  - `spring.ai.ollama.embedding.enabled` (default: `false`)

- **Embedding model selection**
  - `app.ai.embedding-provider` = `ollama` or `openai` (default: `ollama`)

## API

### `GET /chat`

Query params:
- `question` (string, optional) — user prompt
- `topK` (int, optional) — number of documents to retrieve (default: 5)

Example:
```
GET /chat?question=How%20did%20the%20rate%20cut%20impact%20asset%20classes%3F&topK=5
```

### `GET /health`

Health check endpoint.

## Development

Run tests:
```bash
sh ./mvnw test
```

## Troubleshooting

- **`release version 25 not supported`**  
  Ensure your local JDK is set to **Java 25**, or update `pom.xml` to a supported version.

## Notes

- The database schema is reset on startup via `src/main/resources/schema.sql`.
- The demo PDF is packaged at `src/main/resources/docs/article_thebeatoct2024.pdf`.

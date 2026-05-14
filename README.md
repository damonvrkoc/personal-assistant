# Personal assistant (WhatsApp, Spring Boot)

Spring Boot service that receives [WhatsApp Cloud API](https://developers.facebook.com/docs/whatsapp/cloud-api) webhooks, keeps short-term chat history in an in-memory Caffeine cache, persists conversation turns to Neo4j asynchronously, and replies using Spring AI’s `ChatModel` (OpenAI-compatible client by default, swappable via configuration).

## Requirements

- JDK **17+** (the build targets Java 17 bytecode for broad compatibility).
- Maven 3.9+.
- A reachable **Neo4j** instance (local Docker, Neo4j Aura, etc.).
- **Meta WhatsApp Business Platform** app with Cloud API enabled.
- An **LLM endpoint** compatible with the configured Spring AI OpenAI client (OpenAI, many proxies, or Ollama via OpenAI compatibility mode).

## Quick start

1. Start Neo4j and set `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD`.
2. Export WhatsApp Cloud API variables (see below) and `OPENAI_API_KEY` (or use the Ollama profile).
3. Run:

```bash
mvn spring-boot:run
```

4. Expose `https://<public-host>/webhook/whatsapp` to Meta (ngrok, Cloudflare Tunnel, etc.) and complete webhook verification in the Meta developer console.

## Environment variables

| Variable | Purpose |
| --- | --- |
| `WHATSAPP_CLOUD_VERIFY_TOKEN` | Must match the verify token you configure in Meta’s webhook UI. |
| `WHATSAPP_CLOUD_APP_SECRET` | Used to validate `X-Hub-Signature-256` on inbound webhooks. |
| `WHATSAPP_CLOUD_ACCESS_TOKEN` | Bearer token for outbound Graph API calls. |
| `WHATSAPP_CLOUD_PHONE_NUMBER_ID` | WhatsApp phone number id from Meta. |
| `WHATSAPP_CLOUD_API_VERSION` | Optional, default `v21.0`. |
| `WHATSAPP_GRAPH_BASE_URL` | Optional, default `https://graph.facebook.com`. |
| `ALLOWED_WA_IDS` | Optional comma-separated allowlist of WhatsApp user ids (digits, no `+`). When empty, all senders are accepted. |
| `OPENAI_API_KEY` | API key for the default OpenAI-compatible client. |
| `OPENAI_BASE_URL` | Optional override (defaults to `https://api.openai.com`). Set to `http://localhost:11434/v1` for Ollama compatibility. |
| `OPENAI_MODEL` | Optional model name (default `gpt-4o-mini`). |
| `NEO4J_URI` | Bolt URI, default `bolt://localhost:7687`. |
| `NEO4J_USERNAME` / `NEO4J_PASSWORD` | Neo4j credentials. |

## Ollama (OpenAI-compatible) profile

Activate Spring profile `ollama` and point the OpenAI client at Ollama’s OpenAI shim:

```bash
export SPRING_PROFILES_ACTIVE=ollama
export OPENAI_API_KEY=unused
mvn spring-boot:run
```

See [`src/main/resources/application-ollama.yaml`](src/main/resources/application-ollama.yaml).

## Architecture notes

- `GET /webhook/whatsapp` implements Meta’s subscription challenge.
- `POST /webhook/whatsapp` verifies the HMAC signature, parses text messages, and hands them to `AssistantService`.
- `WebhookIdempotencyService` deduplicates `wamid` retries; failed processing releases the id so Meta retries can succeed.
- `ConversationMemoryService` stores recent turns per `wa_id` with TTL and a hard cap on stored messages.
- `TurnPersistenceService` appends `(:WhatsAppUser)-[:HAD_TURN]->(:ConversationTurn)` asynchronously for durable history.

## Tests

```bash
mvn test
```

## Security

Never commit real tokens. Prefer environment variables or a secret manager in production, and rotate the Meta app secret if it is exposed.

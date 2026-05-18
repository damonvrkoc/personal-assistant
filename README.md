# Personal assistant (Slack + WhatsApp, Spring Boot)

Spring Boot service that chats over **Slack** (Socket Mode) and/or **WhatsApp** (Meta Cloud API). Channels are toggled independently. Short-term history lives in a Caffeine cache; conversation turns are persisted to Neo4j asynchronously. Replies use Spring AI’s `ChatModel` (OpenAI-compatible by default).

## Requirements

- JDK **17+**
- Maven 3.9+
- **Neo4j** (local or Aura)
- **Slack app** with Socket Mode (default channel) and/or **Meta WhatsApp Business** app
- An **LLM endpoint** (OpenAI, Ollama via OpenAI compatibility, etc.)

## Quick start (Slack, default)

1. Start Neo4j; set `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD`.
2. Copy [`.env.example`](.env.example) and set `SLACK_BOT_TOKEN`, `SLACK_APP_TOKEN`, `OPENAI_API_KEY`.
3. Run:

```bash
mvn spring-boot:run
```

4. DM the bot in Slack. No public HTTPS URL is required (Socket Mode).

Defaults: `CHANNEL_SLACK_ENABLED=true`, `CHANNEL_WHATSAPP_ENABLED=false`.

## Slack setup (Socket Mode)

1. Create an app at [api.slack.com/apps](https://api.slack.com/apps).
2. Enable **Socket Mode**; create an **App-Level Token** with scope `connections:write` → `SLACK_APP_TOKEN` (`xapp-...`).
3. **OAuth & Permissions** → Bot Token Scopes: `chat:write`, `im:history`, `im:read`, `users:read` (lookup user for startup DM).
4. Install the app to your workspace → `SLACK_BOT_TOKEN` (`xoxb-...`).
5. **Event Subscriptions** → enable and subscribe to bot event **`message.im`** (required even with Socket Mode).
6. Set env vars and run the app. You should see `Slack Socket Mode connected` and a startup recap DM to `damon.vrkoc` (override with `SLACK_STARTUP_NOTIFY_USER` or your Slack user ID `U...`).

## WhatsApp setup (optional)

Set `CHANNEL_WHATSAPP_ENABLED=true` and configure Meta Cloud API variables. Expose `https://<public-host>/webhook/whatsapp` (ngrok, etc.) and complete webhook verification in the Meta developer console.

## Environment variables

| Variable | Purpose |
| --- | --- |
| `CHANNEL_SLACK_ENABLED` | Enable Slack (default `true`). |
| `CHANNEL_WHATSAPP_ENABLED` | Enable WhatsApp (default `false`). |
| `SLACK_BOT_TOKEN` | Bot token (`xoxb-...`) for `chat.postMessage`. |
| `SLACK_APP_TOKEN` | App-level token (`xapp-...`) for Socket Mode. |
| `ALLOWED_SLACK_USER_IDS` | Optional comma-separated Slack user IDs. |
| `WHATSAPP_CLOUD_*` | Meta webhook and Graph API (when WhatsApp enabled). |
| `ALLOWED_WA_IDS` | Optional comma-separated WhatsApp user ids. |
| `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_MODEL` | Spring AI OpenAI client. |
| `NEO4J_URI` / `NEO4J_USERNAME` / `NEO4J_PASSWORD` | Neo4j connection. |

## Ollama profile

```bash
export SPRING_PROFILES_ACTIVE=ollama
export OPENAI_API_KEY=unused
mvn spring-boot:run
```

See [`src/main/resources/application-ollama.yaml`](src/main/resources/application-ollama.yaml).

## Status API and web client

While the app is running (default port **8080**):

| URL | Description |
| --- | --- |
| `GET /api/status` | JSON snapshot (channels, LLM, Neo4j, memory, uptime) |
| [http://localhost:8080/client](http://localhost:8080/client) | Browser UI (Refresh + auto-refresh every 10s) |

```bash
curl http://localhost:8080/api/status
```

No secrets are exposed in the response (only booleans such as `apiKeyConfigured`). The `neo4j` section includes `configured`, `status`, `uri` (no password), `detail` (connectivity error message when DOWN), and `lastPersistence` (`NONE` / `OK` / `FAILED` from async chat writes).

## Architecture

- **Ingress**: Slack `SlackSocketModeRunner` (conditional) or WhatsApp `MetaWebhookController` (conditional).
- **Core**: `ChannelMessage` → `AssistantService` → Caffeine memory + `ChatModel`.
- **Egress**: `OutboundMessenger` per channel (`SlackOutboundMessenger`, `WhatsAppOutboundMessenger`).
- Conversation keys: `slack:U123`, `whatsapp:1555...` (separate history per channel).
- Neo4j: `(:ChannelUser {channel, externalId})-[:HAD_TURN]->(:ConversationTurn)`.

## Tests

```bash
mvn test
```

## Security

Never commit real tokens. Use environment variables or a secret manager in production.

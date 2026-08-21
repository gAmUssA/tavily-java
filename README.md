# tavily-java

A Java SDK for [Tavily](https://tavily.com/)'s web-access API, plus a Quarkus + LangChain4j
agent demo that uses it to answer airline-loyalty questions against the live web.

## What's in this repo

```
tavily-java/
├── tavily-java-sdk/       Type-safe Java client for Tavily's REST API.
│                          Records over /search, /extract, /crawl and /map. JDK 21+.
└── loyalty-agent-demo/    Quarkus app: a LangChain4j agent that uses the
                           SDK as live-web tools and answers loyalty questions
                           via Claude Sonnet. Streams tool calls and the final
                           answer over Server-Sent Events to a minimal web UI.
```

## Why a Java SDK?

Tavily ships official SDKs for Python and TypeScript — nothing for Java.

LangChain4j *does* have a Tavily integration, but it's a `WebSearchEngine` over
`/search` only (`langchain4j-web-search-engine-tavily`, still `-beta`; the
Quarkus extension `quarkus-langchain4j-tavily` wraps the same thing). No
`/extract`, no `/crawl`, no `/map` — so the search → read-the-page loop this demo
runs on isn't reachable through it, and neither is anything else Tavily added
after search.

This repo is the missing building block: a plain Java client for the whole API,
and a worked example of driving an agent with it.

## Prerequisites

- **JDK 21** (LTS). Verify: `java --version`.
- **Maven 3.9+**. Verify: `mvn --version`.
- **Tavily API key** — get one at <https://app.tavily.com/> (1,000 free credits/month).
- **Anthropic API key** — get one at <https://console.anthropic.com/>.

## Quick start

```bash
# 1. Set keys (use .env, or export inline)
export TAVILY_API_KEY=tvly-...
export ANTHROPIC_API_KEY=sk-ant-...

# 2. Build and install the SDK into your local repo
mvn -DskipTests install

# 3. Run the agent in dev mode
mvn -pl loyalty-agent-demo quarkus:dev

# 4. Open the UI
open http://localhost:8080
```

Click one of the three pre-canned queries or type your own. Tool calls and the
final answer stream into the page. Watch the Quarkus terminal for verbose
LangChain4j tool-call traces (Claude → Tavily → Claude → answer).

Port 8080 already taken? `mvn -pl loyalty-agent-demo quarkus:dev -Dquarkus.http.port=8081`.

### Loading from .env

```bash
cp .env.example .env   # then fill in the keys
set -a; source .env; set +a
mvn -pl loyalty-agent-demo quarkus:dev
```

## The three demo queries

| # | Type                        | Query                                                                           | What it shows                                                |
|---|-----------------------------|---------------------------------------------------------------------------------|--------------------------------------------------------------|
| 1 | Fact that recently changed  | *Current MileagePlus earning rate on Polaris business?*                         | LLM training prior is wrong. Search → extract → fresh number with citation. ~14s, 2 tool calls. |
| 2 | Multi-source reasoning      | *120k miles — transfer to Avianca LifeMiles or fly United direct to Tokyo in March?* | Searches across charts + transfer ratios. Markdown comparison table. ~21s, 2–4 tool calls. |
| 3 | Schema-first extraction     | *Find a recent OMAAT devaluation post and show the diff as a markdown table.*   | Long-form post in, structured table out. *Heads-up:* United eliminated fixed award charts in 2019, so the agent will return a policy-change table, not a numeric chart diff. For a cleaner numeric diff, target Hyatt/Hilton/Aeroplan-partner devaluations. |

The **Raw model** toggle in the header re-runs the same question with no tools and
no web access — that's the before/after that makes the point on stage.

**Why search returns snippets, not full pages:** `webSearch` runs at
`search_depth=basic` and deliberately leaves `include_raw_content` off, so results
are titles + URLs + short relevance-ranked snippets. That keeps the agent loop
visible — search returns candidates, then the agent calls `extractUrl`
(`POST /extract` at `extract_depth=advanced`) on the best one, so the audience sees
both tool types fire per query. It's also the cheap path: 1 credit for a basic
search vs. paying to ship full text for five results you'll discard four of.

**Why `include_answer` isn't used:** Tavily can search, read and answer in one
call (`include_answer=true` on `/search`). Wired as a tool, it would let the model
shortcut the whole loop — and the visible search → extract cycle *is* the demo. The
SDK supports it for standalone use; the agent just doesn't get it.

## Tool-call budget

The system prompt caps the agent at four sequential tool calls per question.
Without that ceiling, Claude tends to perfectionism-search past 10 calls and hit
LangChain4j's default `maxSequentialToolsInvocations` limit.
Four is enough for: one search → one extract → one verification search → one
retry-on-failed-fetch extract.

**What that costs:** a basic search is 1 credit, an advanced extract is 2 credits
per 5 successfully-read URLs — so a typical question lands around 3–5 credits, and
the free tier's 1,000 credits/month is a couple hundred demo runs. Pass
`.includeUsage(true)` on any request to get the per-call number back in
`response.usage().credits()`.

## SDK usage

The SDK is a plain Java library — no Quarkus, no LangChain4j dependency. Drop it
into any Java 21+ project:

```xml
<dependency>
    <groupId>com.tavily</groupId>
    <artifactId>tavily-java-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
TavilyClient tavily = TavilyClient.builder()
        .apiKey(System.getenv("TAVILY_API_KEY"))
        .build();

// Search — snippets per result
SearchResponse hits = tavily.search(SearchRequest.builder()
        .query("United MileagePlus Polaris earning rate 2026")
        .maxResults(5)
        .searchDepth(SearchRequest.SearchDepth.BASIC)
        .timeRange(SearchRequest.TimeRange.MONTH)
        .build());

// Read a page as clean markdown
ExtractResponse page = tavily.extract(ExtractRequest.builder()
        .url(hits.results().get(0).url())
        .extractDepth(ExtractDepth.ADVANCED)
        .format(ContentFormat.MARKDOWN)
        .build());

System.out.println(page.results().get(0).rawContent());

// Or let Tavily search, read and answer in one call
SearchResponse answered = tavily.search(SearchRequest.builder()
        .query("What is the current MileagePlus earning rate on Polaris business?")
        .includeAnswer(SearchRequest.AnswerMode.ADVANCED)
        .build());
System.out.println(answered.answer());
```

Crawl and map take plain-English steering instead of path regexes:

```java
// Agentic crawl: follow links that match an instruction, extract as you go
CrawlResponse crawled = tavily.crawl(CrawlRequest.builder()
        .url("https://www.aircanada.com/aeroplan")
        .instructions("pages that describe award chart or partner redemption rates")
        .maxDepth(2)
        .limit(20)
        .build());

// Map: same traversal, URLs only — fast and cheap
MapResponse sitemap = tavily.map(MapRequest.builder()
        .url("https://docs.tavily.com")
        .limit(50)
        .build());
```

Notes on the client:

- Auth is `Authorization: Bearer tvly-…`; every endpoint is POST + JSON.
- Every field carries an explicit `@JsonProperty`, so the snake_case wire format
  survives even if you hand the builder your own `ObjectMapper`.
- Non-2xx responses throw `TavilyException` carrying the status and raw body. A URL
  Tavily *can't read*, though, is not an error — it comes back on a 200 inside
  `failedResults`, which is exactly what the demo's `extractUrl` tool keys off.
- Every call has an `…Async` twin returning `CompletableFuture`.

## How the agent uses the SDK

`loyalty-agent-demo` registers the SDK as two LangChain4j `@Tool` methods on
`TavilyTools`:

- `webSearch(query, recency)` → `tavily.search(...)`, `recency` mapped to `time_range`
- `extractUrl(url)` → `tavily.extract(..., extract_depth=advanced, format=markdown)`

The `LoyaltyAgent` interface is annotated `@RegisterAiService(tools = TavilyTools.class)`.
A system prompt instructs Claude to **always** consult the live web before
answering, cite sources, and present comparisons as markdown tables.

```
┌─────────────┐    ┌─────────────────┐    ┌────────────────┐    ┌────────────┐
│ index.html  │───▶│ AgentResource   │───▶│ LoyaltyAgent   │───▶│ Claude     │
│ (SSE stream)│    │ /agent/stream   │    │ (LangChain4j)  │    │ Sonnet 4.5 │
└─────────────┘    └─────────────────┘    └────────┬───────┘    └────────────┘
                                                   │ tools
                                                   ▼
                                          ┌────────────────┐
                                          │ TavilyTools    │
                                          │  · webSearch   │
                                          │  · extractUrl  │
                                          └────────┬───────┘
                                                   │
                                                   ▼
                                          ┌────────────────┐
                                          │ TavilyClient   │
                                          │ (this SDK)     │
                                          └────────┬───────┘
                                                   │
                                                   ▼
                                          ┌────────────────┐
                                          │ Tavily REST    │
                                          │ api.tavily.com │
                                          └────────────────┘
```

## Configuration

`loyalty-agent-demo/src/main/resources/application.properties`:

```properties
tavily.api.key=${TAVILY_API_KEY:}
quarkus.langchain4j.anthropic.api-key=${ANTHROPIC_API_KEY}
quarkus.langchain4j.anthropic.chat-model.model-name=claude-sonnet-4-5
```

Override the model with `-Dquarkus.langchain4j.anthropic.chat-model.model-name=...`
or by exporting the property.

**Why not a Claude 5 model?** The Claude 5 family rejects `temperature` and
`top_k` as deprecated, but quarkus-langchain4j (≤1.12.x) unconditionally sends
`top_k=40` unless extended thinking is enabled — so `claude-sonnet-5` fails with
an `invalid_request_error`. Stick with `claude-sonnet-4-5` until the extension
makes `top_k` truly optional.

## CLI

With the app running:

```bash
./bin/loyalty-agent "What's the current MileagePlus earning rate on Polaris business?"
# non-default port:
LOYALTY_AGENT_HOST=http://localhost:8081 ./bin/loyalty-agent "..."
```

## Project status

- ✅ SDK: `search`, `extract`, `crawl`, `map` — the full public API surface
- ☐ SDK: `/research` (async research tasks: submit + poll) — not yet covered
- ☐ Streaming — sync + `CompletableFuture` only for now
- ✅ SDK unit tests (19, stubbed `HttpClient`, fixtures captured from live responses)
- ☐ Demo module tests — smoke-tested via `quarkus:dev` against live APIs

## License

MIT.

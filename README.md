# Embabel: The Complete Reference — Companion Code

Source code for the book series *Embabel: The Complete Reference* by Rajkumar Panda.

Every folder is a standalone build. The examples were compiled and their tests run against:

| | Version |
|---|---|
| Embabel Agent | 1.5.3 |
| Spring Boot | 4.1.1 |
| Spring AI | 2.0.1 |
| Java | 21 (Java 25 where noted) |
| Maven | 3.9 or newer |

## Contents

### Volume 1: Foundations, Agents and Planning

| Folder | Chapter | What it shows |
|---|---|---|
| `ch02-raw-call` | 2 | A raw HTTP call to an LLM, with no framework (plain `javac`) |
| `ch07-spring-fundamentals` | 7 | Dependency injection, configuration and events |
| `ch08-spring-boot-fundamentals` | 8 | A Spring Boot REST service with Actuator |
| `ch09-spring-ai-fundamentals` | 9 | `ChatClient`, structured output and tools in Spring AI |
| `ch13-first-embabel-project` | 13 | The first Embabel project (Maven **and** Gradle builds) |
| `ch14-configuration` | 14 | Embabel configuration properties and model roles |
| `p01-greeting-assistant` | 15 | **P1** AI Greeting Assistant |
| `ch16-text-generation` | 16 | Text generation with `Ai` and `PromptRunner` |
| `p02-content-generator` | 17 | **P2** Structured AI Content Generator |
| `ch18-first-agent` | 18 | A first `@Agent` |
| `ch19-invoking-agents` | 19 | Invoking agents from REST and code |
| `ch20-shell` | 20 | The Embabel shell |
| `p03-customer-support-agent` | 21 | **P3** Customer Support Agent |
| `part04-core-concepts` | 22–30 | Actions, goals, conditions, domain objects, blackboard, context, processes |
| `part05-planning` | 31–36 | GOAP, Utility, Hybrid and Supervisor planning, autonomy |
| `p04-research-agent` | 37 | **P4** Research Agent |

### Volume 2: Tools, Multi-Agent Systems, RAG and State

| Folder | Chapter | What it shows |
|---|---|---|
| `p05-tool-using-agent` | 39–48 | **P5** Tool-Using Business Agent: tools, tool groups, context, security |
| `part07-multi-agent` | 49–51 | Subagents, supervisors, nested processes and failure handling |
| `p09-purchase-approval` | 52 | **P9** Human-in-the-Loop Approval Agent |
| `p06-customer-service-platform` | 53 | **P6** Multi-Agent Customer Service Platform |
| `p07-document-rag-assistant` | 55–62 | **P7** Document RAG Assistant (Lucene, ingestion, `ToolishRag`) |
| `p08-agentic-rag-assistant` | 60, 61, 63 | **P8** Agentic RAG Knowledge Assistant |
| `p10-persistent-stateful-agent` | 64–69 | **P10** Persistent Stateful Agent (`@State`, JDBC persistence, snapshots) |

### Volume 3: LLM Engineering, Safety, MCP, Production and Enterprise

| Folder | Chapter | What it shows |
|---|---|---|
| `part10-llm-engineering` | 70–80 | Model choice and mixing, provider setup, custom models and BYOK, prompt contributors and templates, structured output, SSE streaming, reasoning, tool-loop callbacks and auditing, cost tracking and budgets |
| `part11-safety` | 81–84 | Guardrails, prompt/tool/indirect injection and their defences, PII pseudonymisation, RAG poisoning, JWT security, `@SecureAgentTool`, rate limiting |
| `p11-mcp-server` | 85, 86, 88, 89 | **P11** Embabel MCP Server: exported goals, plain tools and a resource over Streamable HTTP; JWT security, OAuth discovery metadata; the same agents over A2A, secured |
| `p12-mcp-client` | 87, 88 | **P12** Embabel MCP Client: allowlisted `McpToolGroup`, metadata allowlist, `McpToolFactory`, per-user token relay, lazy initialisation, tested against a misbehaving stand-in server |
| `part13-production` | 90–102 | Production: unit, integration and evaluation tests; traces, metrics and structured logs; failure handling, cancellation and timeouts; threading and Java 25; REST API with async and SSE progress; web page; signed webhooks and events; cost measurement; Docker image, Kubernetes manifests, shutdown draining |
| `p13-multi-tenant-rag` | 103–108 | **P13** Multi-Tenant RAG Platform: tenants from JWT claims, tenant context for agent, tools and logs, one index filtered by tenant and audience, per-plan models, daily budgets, isolation tests |

`part10-llm-engineering`, `part11-safety`, `p11-mcp-server`, `p12-mcp-client`, `part13-production` and `p13-multi-tenant-rag` run entirely offline. Their `lab` Spring profile registers scripted "lab" models (in Part XI, a deliberately naive one that obeys injected instructions) as real Embabel `LlmService` beans, so prompts, tool calls, retries, streaming and cost events go through Embabel's genuine pipeline without an API key. The replies are scripted, not generated.

P11 and P12 talk over real HTTP with the MCP Java SDK. P11's tests sign their own JWTs, and `mvn spring-boot:run -Dspring-boot.run.profiles=lab` in `p11-mcp-server` prints a short-lived token for trying the server with any MCP client.

More Volume 3 code (Chapters 109–122, projects P14 and P15) will be added as the chapters are written.

## Running the tests

The tests stub the model, so they run offline with no API key:

```bash
cd p05-tool-using-agent
mvn test
```

To run an application against a real model, set a provider key in the environment (never in a committed file), for example:

```bash
export OPENAI_API_KEY=...     # or ANTHROPIC_API_KEY, or use a local model via Ollama
mvn spring-boot:run
```

Chapter 12 of the book covers the development environment and provider setup.

## A note on outputs

Output shown in the book as **Captured** was printed by these tests. Model answers shown as **illustrative** were not produced by a live model, and a real model's wording will differ.

## Licence

The code is released under the [MIT License](LICENSE).

The book's text is copyright © 2026 Rajkumar Panda and is not part of this repository. NorthWind Outfitters and all its data are fictional. This project is independent and is not endorsed by Embabel Pty Ltd, Broadcom or Oracle.

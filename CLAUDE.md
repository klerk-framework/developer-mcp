# CLAUDE.md

This file provides guidance to AI when working with code in this repository.

## What this is

An MCP (Model Context Protocol) server, written in Kotlin/Ktor, that helps AI assistants write code against the **Klerk framework** (a Kotlin library that replaces the DB + business-logic layers of an information system). It exposes Klerk documentation as MCP resources and exposes code-generation tools so the assistant produces idiomatic Klerk code with correct signatures.

This server is itself consumed by an AI — when editing it, remember the output is read by another LLM, not a human. Tool descriptions, instructions, and generated code snippets are the user interface.

## Commands

```bash
./gradlew build                                  # compile + test + shadow jar
./gradlew test                                   # tests only
./gradlew test --tests "GenerateFunctionCreateModelsTest.generateFunctionCreateModel"   # single test
./gradlew run                                    # run via Gradle (needs SOURCES_PATH set)
java -jar build/libs/klerk-mcp-kt-all.jar        # run the shadow jar
```

Required env: `SOURCES_PATH` — absolute path to the directory containing sibling Klerk repos (e.g. `klerk-web`) whose `/docs` folders are mounted as MCP resources at startup. Missing/invalid paths fail fast (`error(...)` / `check(...)` in `Application.kt`).

Optional env: `KLERK_MCP_PORT` — if set, runs as streamable HTTP on that port (dev mode, supports breakpoints + MCP Inspector); if unset, runs over stdio (production mode, how Junie/Claude Desktop launch it).

## Architecture

**Entry point:** `src/main/kotlin/Application.kt`. `main()` constructs a single `Server`, registers tools (`addTool*` functions) and resources (`addResources`), then dispatches to `startStdio` or `startHttp` based on `KLERK_MCP_PORT`.

**Three kinds of content the server exposes:**

1. **Instructions** (`Instructions.kt`, `provideInstructions()`) — a single large markdown blob handed to the client when it connects. This is the assistant's primer on Klerk concepts (models, state machines, events, DataContainers, authorization, config) and on how/when to call the tools. Edit this when Klerk's idioms change or when a new tool needs to be discoverable.

2. **Resources** (markdown docs) — built once at startup by `createDocumentationResources()` from the `DocumentationSource` enum in `tool/GetDocumentation.kt`. Each entry is either:
   - `file = "foo.md"` → loaded from `src/main/resources/docs/` (packaged in the jar), or
   - `pathToDocs = "klerk-web/docs"` → walked from `$SOURCES_PATH/...` at runtime, one resource per `.md` file found.

   URI scheme: `/docs/<category-lowercase>/<name-lowercase>`.

3. **Tools** — one file per tool under `src/main/kotlin/tool/`, each exposing a top-level `addToolXxx(mcpServer)` function and a string constant for the tool name. Read-only tools share the `readOnly` `ToolAnnotations` value defined in `tool/Setup.kt`. Code-generation tools return a `CodeSnippet` (see `Misc.kt`) serialized to JSON in the tool result — the snippet bundles `code`, `imports`, and human-readable `instructions` (e.g. "Put this in `DataContainers.kt`").

**Code generators** live in `src/main/kotlin/codegenerator/`. Tools in `tool/` parse MCP arguments and delegate to generators that produce the actual Kotlin source. Keep generation logic in `codegenerator/`, MCP-argument plumbing in `tool/`.

**Junie quirk:** Junie does not yet support MCP resources (see [JUNIE-1606](https://youtrack.jetbrains.com/projects/JUNIE/issues/JUNIE-1606)), so the `get_documentation` tool exists as a wrapper that returns the same content as the resource. When that bug is fixed the tool can be retired — there's a TODO comment in `tool/GetDocumentation.kt` describing the migration.

## Adding a new tool

1. Create `src/main/kotlin/tool/MyTool.kt` with a `const val myTool = "my_tool"` and `fun addToolMyTool(mcpServer: Server)` that calls `mcpServer.addTool(...)`.
2. Register it in `Application.kt`'s `main()`.
3. If it generates code, return a `CodeSnippet` JSON in the `TextContent` (see `GenerateModel.kt` for the pattern) — assistants rely on the `imports` and `instructions` fields.
4. If it's relevant for the assistant to discover at connect time, mention it in `Instructions.kt`.

## Klerk-specific guidance for code this MCP generates

The full set of rules the assistant should apply to generated Klerk code lives in `Instructions.kt` (Core Concepts, Idiomatic Patterns, Common Mistakes to Avoid). When changing generation logic, keep it consistent with that document — in particular: model properties must wrap values in `DataContainer` subclasses (never raw `String`/`Int`), config is built with named function references (no lambdas), and `kotlin.time.Clock`/`Instant` are preferred over the `kotlinx.datetime` equivalents.

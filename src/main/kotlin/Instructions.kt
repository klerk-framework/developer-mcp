package dev.klerkframework.klerkmcp

import dev.klerkframework.klerkmcp.tool.DocumentationResource
import dev.klerkframework.klerkmcp.tool.getDocumentation

fun provideInstructions() = """
# Klerk Framework — MCP Instructions

You are assisting a developer who is building a system using the **Klerk framework** — a Kotlin library that replaces the traditional database and business-logic layers of an information system. This document describes how Klerk works so you can provide accurate, idiomatic assistance.

---

## Core Concepts

### Configuration


### Models
A **model** is a data class that represents a domain entity (e.g. `Author`, `Book`). Models are managed by Klerk and always have an associated state. 
Model properties must be wrapped in user defined classes that extends **DataContainer** subclasses (e.g. `StringContainer`, `IntContainer`, `BooleanContainer`), never plain Kotlin types. This enables built-in validation, authorization, and UI labelling.
Use the resource ${DocumentationResource.Models.uri} to access documentation about models.


### State Machines
Every model type has a **state machine** that defines its lifecycle. A state machine has:
- A **void state** — handles events that create new model instances (no model exists yet).
- One or more **instance states** — handle events on existing model instances.


### Events
Events are the only way to mutate data. There are two kinds:
- **Void events** — target no existing model instance (used to create models).
- **Instance events** — target a specific existing model instance.

Events may or may not carry **parameters** (a data class whose properties are also `DataContainer` subclasses).

```kotlin
object CreateAuthor : VoidEventWithParameters<Author, CreateAuthorParams>(Author::class,
        EXTERNAL, CreateAuthorParams::class)
data class CreateAuthorParams(val firstName: FirstName, val lastName: LastName)

data class CreateAuthorParams(
    val firstName: FirstName,
    val lastName: LastName,
 )
```

### Commands
A **command** wraps an event and is submitted via `klerk.handle()`. It carries the event, optional parameters, an optional target model ID, and a context.

```kotlin
klerk.handle(Command(
                event = CreateAuthor,
                model = null,
                params = CreateAuthorParams(
                    firstName = FirstName("Astrid),
                    lastName = LastName("Lindgrén),
                ),
            ),
            Context.system(),
            ProcessingOptions(CommandToken.simple()),
        )
```

`handle()` returns a `CommandResult` which is either a success or a typed problem.

### Reading Data
Data is read via `klerk.read()` (or `klerk.readSuspend()` for coroutines). All reads are authorization-checked.

```kotlin
val astrid = // some model ID
val model = klerk.read(context) { get(astrid) }
val allAuthors = klerk.read(context) { list(collections.authors.all) }
```

### Configuration
Everything is wired together in a `Config` built with `ConfigBuilder`. The config declares (among other things):
- **Managed models** — each model type paired with its state machine and collection views.
- **Authorization rules** — positive and negative rules for reading models, reading properties, executing commands, and reading the event log.
- **Persistence** — `RamStorage` (in-memory, for tests) or `SqlPersistence`.

The configuration is typically built by passing function references to `ConfigBuilder`.

The configuration is passed when starting the server. This means that the configuration cannot change after the server is started. Klerk is 
responsible for upholding all the rules declared in the config. This means that the caller can be confident that the authorization rules are enforced and that
there will be no data corruption when interacting with Klerk.

### Authorization
Authorization is expressed as sets of **positive** and **negative** rule functions. A request is allowed if at least one positive rule allows it and no negative rule denies it.

### Collections / Model Views
`ModelViews` defines how a model type's instances are organized into queryable collections (e.g. `AllModelView`, custom filtered views). Collections are declared in a data class and passed into `ConfigBuilder`.

Access the underlying value via `.value` (authorization-checked) or `.valueWithoutAuthorization` (bypasses auth, use carefully).

### Jobs
Long-running or deferred work is modelled as `RunnableJob` instances scheduled via `klerk.jobs.schedule(job)`. Jobs can also be triggered from within state machine executables.

### Key-Value Store
Klerk provides a built-in key-value store (`klerk.keyValueStore`) for storing strings, integers, and blobs with optional TTL.

### Audit Log / Event Log
Every command that mutates state is recorded. The event log can be queried via `klerk.eventLog.getEventsInAuditLog(context, modelId)`.

---

## Idiomatic Patterns

- **Never use plain Kotlin types** (`String`, `Int`, etc.) directly as model or parameter properties — always wrap them in a `DataContainer` subclass.
- **All mutations go through events and commands** — never modify models directly.
- **Validation belongs in the state machine's `event { }` block** or inside the `DataContainer` subclass, not in ad-hoc application code.
- **Authorization belongs in the config's `authorization { }` block** as rule functions, not scattered through application code.
- **Context** (`KlerkContext`) carries the actor identity and any request-scoped data needed by authorization rules. It is always passed to `handle()` and `read()`.
- **`RamStorage`** is the right choice for tests; **`SqlPersistence`** for production.
- State machine states are defined as a Kotlin `enum class`.

---

## Common Mistakes to Avoid

- Note that the Kotlin standard library now contains kotlin.time.Clock and kotlin.time.Instant. These should be used. Do not use kotlinx.datetime.Clock or kotlinx.datetime.Instant. 
- Do not use raw types for model properties — always subclass a `DataContainer`.
- Do not call `klerk.meta.start()` more than once.
- Do not forget to declare events in the state machine's `event { }` block at the top level of the state machine (in addition to handling them inside states).

## How to use this MCP

### Read the documentation
If your task is in any way related to Klerk you should use the tool '$getDocumentation' and ask for one or more topic. The available topics are:
${DocumentationResource.entries.map { "- ${it.uri}\n" } }

### Generating code
When developing the configuration, you will most likely create at least one function and then put a reference to that function in the configuration. It 
 is important that these functions have the correct signature. There are three ways to get the correct signature (starting :
  1. You can try to first add a reference in the configuration and then use IntelliJ's quick-fix to create the function. However, it seems that 
  there is currently a bug in IntelliJ that prevents this from working.
  2. Use a MCP tool to create the function. There are tools for most of the functions. All these tools have names that starts with "$generateFunctionNamePrefix".
  3. Add a reference in the configuration and then carefully examine which signature is required.

""".trimIndent()

# Klerk-web

Klerk-web is a collection of tools that help you build SSR (server-side rendered) web applications using the Ktor framework. You don't have to 
use all tools, e.g. if you want to use client-side rendering for the users but an SSR admin-UI, you can use klerk-web for
only that part.

## Dependency
```kotlin
implementation("dev.klerkframework:klerk-web")
```

## Context
As you know, interactions with Klerk almost always require a context. We therefore need a way to create a context 
from a Ktor call. The recommended way is to create an extension function that returns a context:
```kotlin
suspend fun ApplicationCall.context(klerk: Klerk<Ctx, Views>): Ctx {
    // your code here
}
```

## Tools

### Admin-UI

The Admin-UI provides a web-interface to manage your system. To use it, you first need to create
a `LowCodeConfig`:
```kotlin
val lowCodeConfig = LowCodeConfig(
    basePath = "/admin",
    contextProvider = ApplicationCall::context,
    showOptionalParameters = { eventReference -> false },
    cssPath = "https://unpkg.com/almond.css@latest/dist/almond.min.css",
    knownAlgorithms = setOf()
)
```

Then you use the config to register the routes:
```kotlin
    routing {
        apply(LowCodeMain(klerk, lowCodeConfig).registerRoutes())
        // other routes
    }
```

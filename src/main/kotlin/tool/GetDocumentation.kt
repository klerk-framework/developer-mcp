package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.documentationContent
import dev.klerkframework.devmcp.tool.DocumentationCategory.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

const val getDocumentation = "get_documentation"
const val DOCUMENT_URI = "document_uri"

/* This is a wrapper for resources. Junie cannot use resources, see https://youtrack.jetbrains.com/projects/JUNIE/issues/JUNIE-1606/Support-MCP-resources
When it is fixed, we should probably do something like this:
fun addDocumentationResources(mcpServer: Server) {
    DocumentationResource.entries.forEach {
        mcpServer.addResource(it.uri, it.name, it.description, "text/markdown", createReadHandler(it))
    }
}

 */

fun addToolGetDocumentation(mcpServer: Server) {
    mcpServer.addTool(
        name = getDocumentation,
        description = "Read documentation about Klerk and its features. Available resources: ${
            DocumentationResource.entries.joinToString(
                ", "
            ) { it.uri }
        }",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(DOCUMENT_URI, buildJsonObject {
                    put("type", "string")
                    put("description", "The id of the documentation resource to read. For example, /docs/config/models")
                })
            },
            required = listOf(DOCUMENT_URI)
        ),
    ) { request ->
        val resourceUri = request.arguments?.get(DOCUMENT_URI)?.jsonPrimitive?.content ?: return@addTool CallToolResult(
            isError = true,
            content = listOf(TextContent("$DOCUMENT_URI must be provided. Available resources: ${DocumentationResource.entries.joinToString(", ")}"))
        )
        val resource =
            DocumentationResource.entries.find { it.uri == resourceUri } ?: return@addTool CallToolResult(
                isError = true,
                content = listOf(TextContent("Resource not found: $resourceUri. Available resources: ${DocumentationResource.entries.joinToString(", ")}"))
            )
        val content = documentationContent[resource] ?: error("Resource not found: ${resource.uri}")
        log.info("Responded with md ${resource.file}")
        CallToolResult(content = listOf(TextContent(content)))
    }
}

enum class DocumentationResource(val category: DocumentationCategory, val description: String, val file: String) {
    Dependencies(DEPENDENCIES, "Describes how to add Klerk dependencies to your project", "dependencies.md"),
    Models(CONFIG, "Describes how models work in Klerk", "models.md"),
    StateMachines(CONFIG, "Describes how state machines work in Klerk", "statemachines.md"),
    KlerkWeb(KLERKWEB, "Describes how to use KlerkWeb", "klerkweb.md"),
    ;

    val lowercaseName: String = name.lowercase()
    val uri: String = "/docs/${category.name.lowercase()}/$lowercaseName"
}

enum class DocumentationCategory {
    CONFIG, USAGE, DEPENDENCIES, KLERKWEB,
}

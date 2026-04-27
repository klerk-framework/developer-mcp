package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.documentationResources
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
        description = "Read markdown documentation about Klerk and its features. Available URIs: ${
            documentationResources.joinToString(", ") { it.uri }
        }",
        toolAnnotations = readOnly,
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(DOCUMENT_URI, buildJsonObject {
                    put("type", "string")
                    put(
                        "description",
                        "The URI of the documentation resource to read. For example, /docs/config/models"
                    )
                })
            },
            required = listOf(DOCUMENT_URI)
        ),
    ) { request ->
        val resourceUri = request.arguments?.get(DOCUMENT_URI)?.jsonPrimitive?.content ?: return@addTool CallToolResult(
            isError = true,
            content = listOf(TextContent("$DOCUMENT_URI must be provided)}"))
        )
        val resource = documentationResources.find { it.uri == resourceUri } ?: return@addTool CallToolResult(
            isError = true,
            content = listOf(TextContent("Resource not found: $resourceUri"))
        )
        log.info("Responded with md ${resource.uri}")
        CallToolResult(content = listOf(TextContent(resource.mdContent)))
    }
}

enum class DocumentationSource(
    val category: DocumentationCategory,
    val description: String,
    val file: String? = null,
    val pathToDocs: String? = null
) {
    Dependencies(DEPENDENCIES, "Describes how to add Klerk dependencies to your project", "dependencies.md"),
    Models(CONFIG, "Describes how models work in Klerk", "models.md"),
    StateMachines(CONFIG, "Describes how state machines work in Klerk", "statemachines.md"),
    KlerkWeb(KLERKWEB, "Describes how to use KlerkWeb", pathToDocs = "klerk-web/docs"),
    ;

    val lowercaseName: String = name.lowercase()
    val uri: String = "/docs/${category.name.lowercase()}/$lowercaseName"
}

enum class DocumentationCategory {
    CONFIG, USAGE, DEPENDENCIES, KLERKWEB,
}

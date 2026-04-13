package dev.klerkframework.devmcp

import dev.klerkframework.devmcp.tool.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredResource
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcpStatelessStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Resource
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

val documentationContent: Map<DocumentationResource, String> = createDocumentationContent()

fun main() {

    val port = System.getenv("KLERK_MCP_PORT")?.toIntOrNull()
    val mcpServer = Server(
        serverInfo = Implementation(
            name = "example-server",
            version = "1.0.0",
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(),
                resources = ServerCapabilities.Resources(),
            ),
        ),
        instructionsProvider = ::provideInstructions,
    )

    addToolGetDocumentation(mcpServer)
    addToolDescribeErrorCode(mcpServer)
    addToolSetupKlerk(mcpServer)
    addToolGenerateFunctionCreateModel(mcpServer)
    addToolGenerateBasicConfig(mcpServer)
    addToolGenerateModel(mcpServer)
    addToolGenerateDataContainer(mcpServer)
    addResources(mcpServer)

    if (port != null) {
        startHttp(mcpServer, port)
    } else {
        startStdio(mcpServer)
    }

}

fun addResources(mcpServer: Server) {
    mcpServer.addResources(
        DocumentationResource.entries.map {
            RegisteredResource(Resource(
                name = it.lowercaseName,
                description = it.description,
                uri = it.uri,
                size = documentationContent[it]?.length?.toLong(),
            )) { request ->
                log.info("Responded with md ${it.file}")
                ReadResourceResult(listOf(TextResourceContents(
                    text = documentationContent[it] ?: error("Resource not found: ${it.uri}"),
                    uri = it.uri,
                    mimeType = "text/markdown",
                )))
            }
        }.toList()
    )
}

fun startHttp(mcpServer: Server, port: Int) {
    println("Starting streaming HTTP server on port $port")
    embeddedServer(CIO, port = port) {
        install(ContentNegotiation) {
            json(McpJson)
        }
        mcpStatelessStreamableHttp {
            mcpServer
        }
    }.start(wait = true)
}

/**
 * Create a transport using standard IO for server communication
 */
fun startStdio(mcpServer: Server) {
    println("Starting server on stdin/stdout")
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered(),
    )

    runBlocking {
        val session = mcpServer.createSession(transport)
        val done = Job()
        session.onClose {
            done.complete()
        }
        done.join()
    }

}

val json = Json { prettyPrint = true }

private fun createDocumentationContent(): Map<DocumentationResource, String> {
    val result = mutableMapOf<DocumentationResource, String>()
    DocumentationResource.entries.forEach {
        val content = object {}.javaClass.getResourceAsStream("/docs/${it.file}")
            ?.bufferedReader()
            ?.readText()
            ?: error("Resource not found: /docs/${it.file}")
        result[it] = content
    }
    return result
}

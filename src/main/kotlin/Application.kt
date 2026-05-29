package dev.klerkframework.devmcp

import dev.klerkframework.devmcp.tool.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.walk

private val log = KotlinLogging.logger {}

val pathToSources = System.getenv("SOURCES_PATH") ?: error(
    "The environment variable 'SOURCES_PATH' must be set. " +
            "It should point to the root of the sources so that mcp can read the /docs in the klerk repositories."
)

val documentationResources: Set<DocumentationResource> = createDocumentationResources()

// TODO: AI hade svårt att förstå att en bakgrundstråd skulle använda Ctx.system(). Det borde dokumenteras i klerk/docs?

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
    //addToolGenerateModelFromTableSchema(mcpServer)
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
        documentationResources.map {
            RegisteredResource(
                Resource(
                    name = it.name,
                    description = it.description,
                    uri = it.uri,
                    size = it.mdContent.length.toLong(),
                )
            ) { _ ->
                readDocumentationResource(it)
            }
        }.toList()
    )
}

private fun readDocumentationResource(resource: DocumentationResource): ReadResourceResult {
    log.info("Responded with resource ${resource.uri}")
    return ReadResourceResult(
        listOf(
            TextResourceContents(
                text = resource.mdContent,
                uri = resource.uri,
                mimeType = "text/markdown",
            )
        )
    )
}

fun startHttp(mcpServer: Server, port: Int) {
    println("Starting streaming HTTP server on port $port")
    embeddedServer(CIO, port = port) {
        install(ContentNegotiation) {
            json(McpJson)
        }
        routing {
            get("/mcp") { call.respondText(helloHtmlResponse(port), ContentType.Text.Html) }
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

private fun createDocumentationResources(): Set<DocumentationResource> {
    val result = mutableSetOf<DocumentationResource>()
    DocumentationSource.entries.forEach { ds ->
        if (ds.file != null) {
            val content = object {}.javaClass.getResourceAsStream("/docs/${ds.file}")
                ?.bufferedReader()
                ?.readText()
                ?: error("Resource not found: /docs/${ds.file}")
            result.add(DocumentationResource(ds.name, ds.category, ds.description, content, ds.uri))
        } else if (ds.pathToDocs != null) {
            val path = Path.of("$pathToSources/${ds.pathToDocs}")
            check(path.exists()) { "Path to docs does not exist: $path" }
            path.walk().forEach { path ->
                val content = Files.readString(path)
                val nameFromFilename = path.fileName.toString().lowercase().replace(".md", "")
                val uri = "/docs/${ds.category.name.lowercase()}/$nameFromFilename"
                result.add(
                    DocumentationResource(
                        "${ds.name}-$nameFromFilename",
                        ds.category,
                        "Docs about $nameFromFilename in ${ds.lowercaseName}",
                        content,
                        uri
                    )
                )
            }
        } else {
            error("Documentation source ${ds.name} must have either a file or a pathToDocs")
        }
    }
    println("Added ${result.size} documentation resources:")
    result.forEach { println(it.uri) }
    return result
}

data class DocumentationResource(
    val name: String,
    val category: DocumentationCategory,
    val description: String,
    val mdContent: String,
    val uri: String,
) {
}

private fun helloHtmlResponse(port: Int) = """
    <html>
        <head>
            <title>MCP Server</title>
        </head>
        <body>
            <h1>Klerk developer MCP server</h1>
            <p>You cannot interact with this MCP with a browser, you should instead use a MCP client. The server is running on port $port. 
            <p>In IntellJ → Settings → Junie → MCP settings:
            <p>
            <pre>
            <code>
{
  "mcpServers": {
    "klerk-dev": {
      "type": "streamable-http",
      "url": "http://localhost:$port/mcp",
      "note": "For Streamable HTTP connections, add this URL directly in your MCP Client"
    }
  }
}
</code></pre>
            <p>For exploration, you can try:</p>
          <p><code>npx -y @modelcontextprotocol/inspector --connect http://localhost:$port</code>
        </body>
    </html>
"""

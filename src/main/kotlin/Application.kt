package com.example

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import com.example.tool.addToolGenerateFunctionCreateModel
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("KLERK_MCP_PORT")?.toIntOrNull()
    val mcpServer = Server(
        serverInfo = Implementation(
            name = "example-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        ),
        instructionsProvider = ::provideInstructions
    )

    addToolGenerateFunctionCreateModel(mcpServer)

    if (port != null) {
        startHttp(mcpServer, port)
    } else {
        startStdio(mcpServer)
    }

}

fun startHttp(mcpServer: Server, port: Int) {
    println("Starting server on port $port")
    embeddedServer(CIO, port = port) {
        mcp {       // TODO: make it work with mcpStreamableHttp ? I had some problems...
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

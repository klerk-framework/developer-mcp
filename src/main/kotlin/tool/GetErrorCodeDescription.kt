package dev.klerkframework.devmcp.tool

import dev.klerkframework.klerk.KlerkErrorCode
import dev.klerkframework.klerk.KlerkErrorCode.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

const val describeErrorCode = "describe_error_code"
const val errorCode = "error_code"

fun addToolDescribeErrorCode(mcpServer: Server) {
    mcpServer.addTool(
        name = describeErrorCode,
        description = "Describes a Klerk error code.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(errorCode, buildJsonObject {
                    put("type", "string")
                    put("description", "The error code. E.g. 'ERROR-CONFIG-4'")
                })
            },
            required = listOf(errorCode)
        )
    ) { request ->
        val codeString = request.arguments?.get(errorCode)?.jsonPrimitive?.content ?: error("No error code provided")
        val cleanedCodeString = codeString.replace("[", "").replace("]", "")
        val code = KlerkErrorCode.entries.find { it.code.equals(cleanedCodeString, ignoreCase = true) }
            ?: error("Invalid error code provided")
        CallToolResult(content = listOf(TextContent(describeErrorCode(code))))
    }
}

private fun describeErrorCode(code: KlerkErrorCode): String = when (code) {
    EventNotDeclared -> """
        ${EventNotDeclared.code} indicates a bug in the configuration provided to Klerk. 
        A state machine must declare which events can be used in the state machine. Example: if 
        you have a state machine for a Author which can react to the event CreateAuthor, you should add something like 
        this to the state machine:
        
        event(CreateAuthor) {
        }
        
        In the event block you can specify various validation rules, e.g.
        
        event(CreateAuthor) {
            validateContext(::preventUnauthenticated)
            validateWithParameters(::cannotHaveAnAwfulName)
            validReferences(CreateAuthorParams::favouriteColleague, views.authors.all)
        }
        """.trimIndent()

    MissingValidReferences -> """
        ${MissingValidReferences.code} indicates a bug in the configuration provided to Klerk. 
        An event with a parameter property that is a reference to a model must declare which models can be referenced. E.g.  
        
        ```kotlin
        event(CreateAuthor) {
            validReferences(CreateAuthorParams::favouriteColleague, views.authors.all)
        }
        ```
        
        To fix it, you typically need to pass the views into the funciton that creates the state machine.
        """.trimIndent()
// TODO:  för MissingValidReferences: skriv vilket mcp-verktyg som ska användas

    MissingSystemContextProvider -> """
        ${MissingSystemContextProvider.code} indicates a bug in the configuration provided to Klerk.
        Klerk may execute commands in the background (e.g. if you have a statemachine with after(5.minutes) {...}). And since a context is required when executing a command, Klerk needs a way to create such a context with the SystemIdentity.
        Typically, add this to the configuration:
        systemContextProvider { systemIdentity -> Ctx(systemIdentity) }
        """.trimIndent()

    else -> {
        log.warn { "No description available for ${code.code}" }
        "No description available for ${code.code}" // TODO
    }

}

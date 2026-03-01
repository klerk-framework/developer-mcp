package dev.klerkframework.klerkmcp.tool

import dev.klerkframework.klerkmcp.CodeSnippet
import dev.klerkframework.klerkmcp.json
import dev.klerkframework.klerkmcp.log
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val generateDataContainer = "generate_data_container"

const val dataContainerName = "data_container_name"

fun addToolGenerateDataContainer(mcpServer: Server) {
    mcpServer.addTool(
        name = generateDataContainer,
        description = "Generates code for a new data container that can be used in models or event parameters.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(dataContainerName, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the data container that should be generated.")
                })
            },
        )
    ) { request ->
        val model = request.arguments?.get(dataContainerName)?.jsonPrimitive?.content?.replaceFirstChar { it.uppercaseChar() } ?: error("No $dataContainerName provided")
        // TODO: type
        val generateModelSnippet = CodeSnippet(
            code = """
                class $model(value: String) : StringContainer(value) {
                    override val minLength = 1
                    override val maxLength = 100
                    override val maxLines = 1
                }
        """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.StringContainer"),
            instructions = "Put this code in a file named 'DataContainers.kt'. Make sure the provided imports are in the file."
        )
        log(CallToolResult(content = listOf(TextContent(json.encodeToString(generateModelSnippet)))))
    }
}

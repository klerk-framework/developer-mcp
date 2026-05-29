package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.codegenerator.ContainerType
import dev.klerkframework.devmcp.codegenerator.generateDataContainerSnippet
import dev.klerkframework.devmcp.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val generateDataContainer = "generate_data_container"

const val dataContainerName = "data_container_name"
const val dataContainerType = "data_container_type"

val dataContainerTypeOptions = ContainerType.entries.joinToString(", ") { it.name.lowercase() }

fun addToolGenerateDataContainer(mcpServer: Server) {
    mcpServer.addTool(
        name = generateDataContainer,
        description = "Generates code for a new data container that can be used in models or event parameters.",
        toolAnnotations = readOnly,
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(dataContainerName, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the data container that should be generated.")
                })
                put(dataContainerType, buildJsonObject {
                    put("type", "string")
                    put("description", "The type that the data container should contain. One of: $dataContainerTypeOptions")
                })
            },
        )
    ) { request ->
        val model = request.arguments?.get(dataContainerName)?.jsonPrimitive?.content
            ?.replaceFirstChar { it.uppercaseChar() } ?: error("No $dataContainerName provided")
        val typeString = request.arguments?.get(dataContainerType)?.jsonPrimitive?.content
            ?: error("No $dataContainerType provided")
        val type = ContainerType.entries.find { it.name.equals(typeString, ignoreCase = true) }
            ?: error("Invalid $dataContainerType provided. Expected one of: $dataContainerTypeOptions")
        CallToolResult(content = listOf(TextContent(json.encodeToString(generateDataContainerSnippet(model, type)))))
    }
}


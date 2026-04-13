package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.CodeSnippet
import dev.klerkframework.devmcp.json
import dev.klerkframework.klerk.datatypes.IntContainer
import dev.klerkframework.klerk.datatypes.LongContainer
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
private val typeOptions = ContainerType.entries.joinToString(", ") { it.name.lowercase() }
private const val instructions =
    "Put this code in a file named 'DataContainers.kt' unless explicitly stated otherwise. Make sure the provided imports are in the file."


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
                put(dataContainerType, buildJsonObject {
                    put("type", "string")
                    put("description", "The type that the data container should contain. One of: $typeOptions")
                })
            },
        )
    ) { request ->
        val model = request.arguments?.get(dataContainerName)?.jsonPrimitive?.content
            ?.replaceFirstChar { it.uppercaseChar() } ?: error("No $dataContainerName provided")
        val typeString = request.arguments?.get(dataContainerType)?.jsonPrimitive?.content
            ?: error("No $dataContainerType provided")
        val type = ContainerType.entries.find { it.name.equals(typeString, ignoreCase = true) }
            ?: error("Invalid $dataContainerType provided. Expected one of: $typeOptions")
        CallToolResult(content = listOf(TextContent(json.encodeToString(generateSnippet(model, type)))))
    }
}


private fun generateSnippet(model: String, type: ContainerType): CodeSnippet =
    when (type) {
        ContainerType.String -> CodeSnippet(
            code = """
                class $model(value: String) : StringContainer(value) {
                    override val minLength = 1
                    override val maxLength = 100
                    override val maxLines = 1
                }
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.StringContainer"),
            instructions = instructions
        )

        ContainerType.Int -> CodeSnippet(
            code = """
                class $model(value: Int) : IntContainer(value) {
                    override val min = 0
                    override val max = Int.MAX_VALUE
                }
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.IntContainer"),
            instructions = instructions
        )

        ContainerType.Long -> CodeSnippet(
            code = """
                class $model(value: Long) : LongContainer(value) {
                    override val min = 0L
                    override val max = Long.MAX_VALUE
                }
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.LongContainer"),
            instructions = instructions
        )

        ContainerType.Float -> CodeSnippet(
            code = """
                class $model(value: Float) : FloatContainer(value) {
                    override val min = 0f
                    override val max = Float.MAX_VALUE
                }
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.FloatContainer"),
            instructions = instructions
        )

        ContainerType.Boolean -> CodeSnippet(
            code = """
                class $model(value: Boolean) : BooleanContainer(value)
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.BooleanContainer"),
            instructions = instructions
        )

        ContainerType.GeoPosition -> CodeSnippet(
            code = """
                class $model(value: GeoPosition) : GeoPositionContainer(value)
            """.trimIndent(),
            imports = listOf(
                "dev.klerkframework.klerk.datatypes.GeoPositionContainer",
                "dev.klerkframework.klerk.misc.GeoPosition"
            ),
            instructions = instructions
        )

        ContainerType.Duration -> CodeSnippet(
            code = """
                class $model(value: Duration) : DurationContainer(value)
            """.trimIndent(),
            imports = listOf(
                "dev.klerkframework.klerk.datatypes.DurationContainer",
                "kotlin.time.Duration"
            ),
            instructions = instructions
        )

        ContainerType.Instant -> CodeSnippet(
            code = """
                class $model(value: Instant) : InstantContainer(value)
            """.trimIndent(),
            imports = listOf(
                "dev.klerkframework.klerk.datatypes.InstantContainer",
                "kotlin.time.Instant"
            ),
            instructions = instructions
        )
    }

private enum class ContainerType {
    String, Int, Long, Float, Boolean, GeoPosition, Duration, Instant
}

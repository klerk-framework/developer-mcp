package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.CodeSnippet
import dev.klerkframework.devmcp.codegenerator.ContainerType
import dev.klerkframework.devmcp.codegenerator.generateDataContainers
import dev.klerkframework.devmcp.codegenerator.generateWholeModel
import dev.klerkframework.devmcp.codegenerator.toDataContainerType
import dev.klerkframework.devmcp.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*

const val generateModel = "generate_model"
const val properties = "properties"

fun addToolGenerateModel(mcpServer: Server) {
    mcpServer.addTool(
        name = generateModel,
        description = "Generates code for a new model.",
        toolAnnotations = readOnly,
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(modelName, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the model that should be generated.")
                })
                put(properties, buildJsonObject {
                    put("type", "array")
                    put("description", "The properties of the model.")
                    put("items", buildJsonObject {
                        put("type", "object")
                        put("property", buildJsonObject {
                            put("name", buildJsonObject {
                                put("type", "string")
                                put("description", "The name of the property")
                            })
                            put("type", buildJsonObject {
                                put("type", "string")
                                put(
                                    "description",
                                    "The type that the data container should contain. One of: $dataContainerTypeOptions"
                                )
                            })
                            put("nullable", buildJsonObject { put("type", "boolean") })
                            put("model_reference", buildJsonObject {
                                put("type", "string")
                                put(
                                    "description",
                                    "If the type is ${ContainerType.ModelReference.name}, this should be the name of the referenced model"
                                )
                            })
                            put("default_value", buildJsonObject {
                                put("type", "string")
                                put(
                                    "description",
                                    "The default value for the property. Null if no default value is provided."
                                )
                            })
                        })
                        put("required", buildJsonArray {
                            add("name")
                            add("type")
                            add("nullable")
                        })
                    })
                })
            },
            required = listOf(modelName, properties)
        )
    ) { request ->
        val model =
            request.arguments?.get(modelName)?.jsonPrimitive?.content?.replaceFirstChar { it.uppercaseChar() } ?: error(
                "No $modelName provided"
            )
        check(model.isPascalCase()) { "Model name must be in PascalCase" }

        val propertiesArray = request.arguments?.get(properties)?.jsonArray
            ?: error("No $properties provided")

        val parsedProperties = propertiesArray
            .map { element ->
                val obj = element.jsonObject
                PropertyDefinition(
                    name = obj["name"]!!.jsonPrimitive.content,
                    type = ContainerType.valueOf(obj["type"]!!.jsonPrimitive.content.replaceFirstChar { it.uppercaseChar() }),
                    nullable = obj["nullable"]!!.jsonPrimitive.boolean,
                    fkModel = obj["model_reference"]?.jsonPrimitive?.contentOrNull,
                    defaultValue = obj["default_value"]?.jsonPrimitive?.contentOrNull
                )
            }
            .map { toDataContainerType(it) }
            .toSet()
        val snippets = mutableListOf<CodeSnippet>()
        snippets.add(
            CodeSnippet(
                code = generateWholeModel(model, parsedProperties),
                imports = listOf(
                    "import dev.klerkframework.klerk.*",
                    "import dev.klerkframework.klerk.statemachine.StateMachine",
                ),
                instructions = "Put this code in a file named '$model.kt' in the package 'models'. Make sure the provided imports are in the file."
            )
        )

        snippets.add(generateDataContainers(parsedProperties))

        CallToolResult(content = snippets.map { TextContent(json.encodeToString(it)) })
    }
}

private fun String.isPascalCase(): Boolean {
    return matches(Regex("^[A-Z][a-zA-Z0-9]*$"))
}


data class PropertyDefinition(
    val name: String,
    val type: ContainerType,
    val nullable: Boolean,
    val fkModel: String?,
    val defaultValue: String?,
)

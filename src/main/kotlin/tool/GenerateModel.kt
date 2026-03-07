package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.CodeSnippet
import dev.klerkframework.devmcp.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val generateModel = "generate_model"

fun addToolGenerateModel(mcpServer: Server) {
    mcpServer.addTool(
        name = generateModel,
        description = "Generates code for a new model.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(modelName, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the model that should be generated.")
                })
                put(dataContainerName, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of an existing data container that should be used in the model.")
                })
            },
            required = listOf(modelName, dataContainerName)
        )
    ) { request ->
        val model =
            request.arguments?.get(modelName)?.jsonPrimitive?.content?.replaceFirstChar { it.uppercaseChar() } ?: error(
                "No $modelName provided"
            )
        val dataContainer =
            request.arguments?.get(dataContainerName)?.jsonPrimitive?.content?.replaceFirstChar { it.uppercaseChar() }
                ?: error("No $dataContainerName provided")
        val generateModelSnippet = CodeSnippet(
            code = """
            data class ${model}(
                val ${dataContainer.lowercase()}: $dataContainer,
            )
            
            enum class ${model}States {
                Updatable,
            }
            
            fun create${model}StateMachine(): StateMachine<${model}, ${model}States, Ctx, Collections> =
                stateMachine {
            
                    event(Create${model}) { }
                    event(Update${model}) { }
                    event(Delete${model}) { }
            
                    voidState {
                        onEvent(Create${model}) {
                            createModel(initialState = ${model}States.Updatable, ::create${model})
                        }
                    }
            
                    state(${model}States.Updatable) {
                        onEvent(Update${model}) {
                            update(::update${model})
                        }
            
                        onEvent(Delete${model}) {
                            delete()
                        }
                    }
            
                }
            
            object Create${model} : VoidEventWithParameters<${model}, ${model}>(${model}::class, EventVisibility.CODE, ${model}::class)
            
            object Update${model} : InstanceEventWithParameters<${model}, ${model}>(${model}::class, EventVisibility.CODE, ${model}::class)
            
            object Delete${model} : InstanceEventNoParameters<${model}>(${model}::class, EventVisibility.CODE)
            
            fun create${model}(args: ArgForVoidEvent<${model}, ${model}, Ctx, Collections>): ${model} {
                return args.command.params
            }
            
            fun update${model}(args: ArgForInstanceEvent<${model}, ${model}, Ctx, Collections>): ${model} {
                return args.command.params
            }

        """.trimIndent(),
            imports = listOf(
                "import dev.klerkframework.klerk.*",
                "import dev.klerkframework.klerk.statemachine.StateMachine",
                "import dev.klerkframework.klerk.statemachine.stateMachine",
                ),
            instructions = "Put this code in a file named '$model.kt' in the package 'models'. Make sure the provided imports are in the file."
        )
        CallToolResult(content = listOf(TextContent(json.encodeToString(generateModelSnippet))))
    }
}

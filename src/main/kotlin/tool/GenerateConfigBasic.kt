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

val generateBasicConfig = "generate_basic_config"

fun addToolGenerateBasicConfig(mcpServer: Server) {
    mcpServer.addTool(
        name = generateBasicConfig,
        description = "Generates code for a basic configuration.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(modelName, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the a model that should be included in the configuration.")
                })
            },
        )
    ) { request ->
        val model =
            request.arguments?.get(modelName)?.jsonPrimitive?.content?.replaceFirstChar { it.uppercaseChar() } ?: error(
                "No $modelName provided"
            )
        CallToolResult(content = listOf(TextContent(json.encodeToString(createBasicConfigSnippet(model)))))
    }
}

private fun createBasicConfigSnippet(model: String) = CodeSnippet(
    code = """
        import dev.klerkframework.klerk.*
        import dev.klerkframework.klerk.collection.ModelViews
        import dev.klerkframework.klerk.storage.RamStorage
        import kotlin.time.Clock
        import kotlin.time.Instant

        class Ctx(
            override val actor: ActorIdentity,
            override val auditExtra: String? = null,
            override val time: Instant = Clock.System.now(),
            override val translation: Translation = DefaultTranslation,
        ) : KlerkContext

        data class Views(
            val ${model.lowercase()}s: ModelViews<$model, Ctx>,
        )

        fun createConfig(): Config<Ctx, Views> {
            val views = Views(ModelViews())
            return ConfigBuilder<Ctx, Views>(views).build {
                persistence(RamStorage())
                managedModels {
                    model($model::class, create${model}StateMachine(), views.${model.lowercase()}s)
                }
                authorization {
                    insecureAllowEverything()
                }
                systemContextProvider { systemIdentity -> Ctx(systemIdentity) }
            }
        }
        """.trimIndent(),
    imports = emptyList(),
    instructions = "Put the provided code EXACTLY as it is into a file named 'Config.kt'. Important: do not modify the imports! You should then add an import for $model."
)

/*
        private fun createPersistence(): Persistence {
            val dbFilePath =
                requireNotNull(System.getenv("DATABASE_PATH")) { "The environment variable 'DATABASE_PATH' must be set" }
            val ds = SQLiteDataSource()
            ds.url = "jdbc:sqlite:${'$'}dbFilePath"
            return SqlPersistence(ds)
        }

 */

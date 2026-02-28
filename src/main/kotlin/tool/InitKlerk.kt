package com.example.tool

import com.example.CodeSnippet
import com.example.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

private const val FUNCTION_NAME = "function_name"

private const val IN_VOID_STATE = "in_void_state"

private const val MODEL_NAME = "model_name"

private const val PARAMETER_CLASS_NAME = "parameter_class_name"

private const val HAS_PARAMETERS = "has_parameters"

fun createKlerkSetup(mcpServer: Server) {
    mcpServer.addTool(
        name = "create_setup",
        description = "Generates code to setup a basic Klerk project.",
    ) { request ->
        CallToolResult(content = listOf(TextContent(json.encodeToString(createConfigSnippet))))
    }
}

private val createConfigSnippet = CodeSnippet(
    code = """
        class Ctx(
            override val actor: ActorIdentity,
            override val auditExtra: String? = null,
            override val time: Instant = Clock.System.now(),
            override val translation: Translation = DefaultTranslation,
        ) : KlerkContext {

            companion object {

                fun unauthenticated(): Ctx = Ctx(Unauthenticated)

                fun authenticationIdentity(): Ctx = Ctx(AuthenticationIdentity)

                fun system(): Ctx = Ctx(SystemIdentity)
            }

        }

        data class Collections(
            val users: ModelViews<User, Ctx>,
            val games: ModelViews<Game, Ctx>,
        )

        fun createConfig(): Config<Ctx, Collections> {
            val collections = Collections(ModelViews(), ModelViews())
            return ConfigBuilder<Ctx, Collections>(collections).build {
                persistence(createPersistence())
                managedModels {
                    model(User::class, createUserStateMachine(), collections.users)
                    model(Game::class, createGameStateMachine(collections), collections.games)
                }
                apply(createAuthorizationRules())
                systemContextProvider { systemIdentity -> Ctx(systemIdentity) }
            }
        }

        private fun createPersistence(): Persistence {
            val dbFilePath =
                requireNotNull(System.getenv("DATABASE_PATH")) { "The environment variable 'DATABASE_PATH' must be set" }
            val ds = SQLiteDataSource()
            ds.url = "jdbc:sqlite:${'$'}dbFilePath"
            return SqlPersistence(ds)
        }

                
            """.trimIndent(),
    imports = listOf(
        "dev.klerkframework.klerk.*",
        "dev.klerkframework.klerk.collection.ModelViews",
        "dev.klerkframework.klerk.storage.Persistence",
        "dev.klerkframework.klerk.storage.SqlPersistence",
        "org.sqlite.SQLiteDataSource",
        "kotlin.time.Clock",
        "kotlin.time.Instant",
    ),
    instructions = ""
)

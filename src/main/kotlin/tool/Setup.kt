package dev.klerkframework.devmcp.tool

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

const val createBasicSetup = "setup_klerk"

fun addToolSetupKlerk(mcpServer: Server) {
    mcpServer.addTool(
        name = createBasicSetup,
        description = "Setup Klerk: add dependencies, generate a model, generate a basic configuration, and start Klerk.",
    ) { request ->
        CallToolResult(
            content = listOf(
                TextContent(createBasicSetupInstructions),
                //  ResourceLink(DocumentationResource.Dependencies.name, DocumentationResource.Dependencies.uri)
            )
        )
    }
}

private val createBasicSetupInstructions = """
    To setup Klerk, follow these steps:
    1. Edit build.gradle.kts according to the documentation (use the tool '$getDocumentation' and ask for  "${DocumentationResource.Dependencies.uri}"). Download the dependencies with Gradle.
    2. Use the tool $generateDataContainer to generate at least one data container. If it is not clear which data container to generate, use 'Title' as $dataContainerName.   
    2. Use the tool $generateModel to generate a model. If it is not clear which model to generate, use 'Book' as $modelName. Don't try to fix missing imports immediately, they should be fixed in the final step.
    3. Use the tool $generateBasicConfig to generate a basic configuration. Use the same $modelName that was used in the previous step. 
    4. Add code somewhere in the beginning of your application, probably in the file named 'Application.kt':
        runBlocking {
            val klerk = Klerk.create(createConfig())
            klerk.meta.start()
        }
    5. Sync Gradle.    
    6. Fix missing imports.
    7. Make sure that the project compiles.
    """.trimIndent()

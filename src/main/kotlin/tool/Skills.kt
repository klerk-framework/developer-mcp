package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.skills
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

const val listSkills = "list_skills"
const val getSkill = "get_skill"
const val SKILL_NAME = "skill_name"

/*
 Junie cannot use resources (https://youtrack.jetbrains.com/projects/JUNIE/issues/JUNIE-1606),
 so skills are also exposed as tools. When that bug is fixed these tools can be retired in favor
 of the resources registered in Skills.kt.
*/

fun addToolListSkills(mcpServer: Server) {
    mcpServer.addTool(
        name = listSkills,
        description = "List available Klerk skills. A skill is a focused playbook for a specific task " +
                "(e.g. converting an SQL schema to Klerk models). Returns each skill's name and a one-line " +
                "description. Use '$getSkill' to read the full content of a skill that looks relevant.",
        toolAnnotations = readOnly,
    ) { _ ->
        val body = if (skills.isEmpty()) {
            "No skills are currently available."
        } else {
            skills.joinToString("\n") { "- ${it.name}: ${it.description}" }
        }
        CallToolResult(content = listOf(TextContent(body)))
    }
}

fun addToolGetSkill(mcpServer: Server) {
    mcpServer.addTool(
        name = getSkill,
        description = "Read the full markdown content of a Klerk skill. Use '$listSkills' first to " +
                "discover which skills exist. Available skills: ${
                    skills.joinToString(", ") { it.name }
                }",
        toolAnnotations = readOnly,
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(SKILL_NAME, buildJsonObject {
                    put("type", "string")
                    put(
                        "description",
                        "The name of the skill to read, as listed by '$listSkills' " +
                                "(e.g. 'convert-from-sql')."
                    )
                })
            },
            required = listOf(SKILL_NAME)
        ),
    ) { request ->
        val requested = request.arguments?.get(SKILL_NAME)?.jsonPrimitive?.content
            ?: return@addTool CallToolResult(
                isError = true,
                content = listOf(TextContent("$SKILL_NAME must be provided"))
            )
        val skill = skills.find { it.name == requested }
            ?: return@addTool CallToolResult(
                isError = true,
                content = listOf(
                    TextContent(
                        "Skill not found: '$requested'. Known skills: ${
                            skills.joinToString(", ") { it.name }
                        }"
                    )
                )
            )
        log.info("Responded with skill ${skill.uri}")
        CallToolResult(content = listOf(TextContent(skill.content)))
    }
}

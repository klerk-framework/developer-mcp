package dev.klerkframework.devmcp

import io.modelcontextprotocol.kotlin.sdk.server.RegisteredResource
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Resource
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.walk

private val log = KotlinLogging.logger {}

const val SKILLS_RESOURCE_DIR = "skills"

data class Skill(
    val name: String,
    val description: String,
    val content: String,
) {
    val uri: String = "/skills/$name"
}

val skills: List<Skill> = loadSkills()

private fun loadSkills(): List<Skill> {
    val classLoader = object {}.javaClass.classLoader
    val dirUrl = classLoader.getResource(SKILLS_RESOURCE_DIR)
        ?: error("Resource directory '$SKILLS_RESOURCE_DIR' not found on classpath")

    val markdownFiles: List<Pair<String, String>> = when (dirUrl.protocol) {
        "file" -> {
            val root = Path.of(dirUrl.toURI())
            check(root.exists()) { "Skills path does not exist: $root" }
            root.walk()
                .filter { it.extension.equals("md", ignoreCase = true) }
                .map { it.fileName.toString() to Files.readString(it) }
                .toList()
        }

        "jar" -> {
            val jarPath = dirUrl.path.substringAfter("file:").substringBefore("!")
            JarFile(jarPath).use { jar ->
                jar.entries().asSequence()
                    .filter {
                        !it.isDirectory &&
                                it.name.startsWith("$SKILLS_RESOURCE_DIR/") &&
                                it.name.endsWith(".md", ignoreCase = true)
                    }
                    .map { entry ->
                        val text = jar.getInputStream(entry).bufferedReader().readText()
                        entry.name.substringAfterLast('/') to text
                    }
                    .toList()
            }
        }

        else -> error("Unsupported resource protocol for skills: ${dirUrl.protocol}")
    }

    val result = markdownFiles.map { (filename, text) -> parseSkill(filename, text) }
    println("Added ${result.size} skills:")
    result.forEach { println(it.uri) }
    return result
}

private fun parseSkill(filename: String, content: String): Skill {
    val lines = content.lines()
    require(lines.isNotEmpty() && lines.first().trim() == "---") {
        "Skill '$filename' must start with YAML frontmatter delimited by '---'"
    }
    val closingIndex = lines.subList(1, lines.size).indexOfFirst { it.trim() == "---" }
    require(closingIndex >= 0) { "Skill '$filename' has an unclosed frontmatter block" }
    val frontmatterEnd = closingIndex + 1

    val frontmatter = mutableMapOf<String, String>()
    for (line in lines.subList(1, frontmatterEnd)) {
        val colon = line.indexOf(':')
        if (colon <= 0) continue
        val key = line.substring(0, colon).trim()
        val value = line.substring(colon + 1).trim().removeSurrounding("\"").removeSurrounding("'")
        frontmatter[key] = value
    }

    val name = frontmatter["name"]
        ?: error("Skill '$filename' is missing required frontmatter field 'name'")
    val description = frontmatter["description"]
        ?: error("Skill '$filename' is missing required frontmatter field 'description'")

    return Skill(name = name, description = description, content = content)
}

fun addSkillResources(mcpServer: Server) {
    mcpServer.addResources(
        skills.map { skill ->
            RegisteredResource(
                Resource(
                    name = skill.name,
                    description = skill.description,
                    uri = skill.uri,
                    size = skill.content.length.toLong(),
                )
            ) { _ ->
                log.info("Responded with skill ${skill.uri}")
                ReadResourceResult(
                    listOf(
                        TextResourceContents(
                            text = skill.content,
                            uri = skill.uri,
                            mimeType = "text/markdown",
                        )
                    )
                )
            }
        }
    )
}

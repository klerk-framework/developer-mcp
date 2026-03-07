package dev.klerkframework.devmcp

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import kotlinx.serialization.Serializable

@Serializable
data class CodeSnippet(
    val code: String,
    val imports: List<String>,
    val instructions: String
)

fun capFirstLetter(s: String): String = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

const val generateFunctionNamePrefix = "generate_function_"

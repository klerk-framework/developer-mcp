package dev.klerkframework.klerkmcp

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

fun log(result: CallToolResult): CallToolResult {
    result.content.forEach { println(it) }
    return result
}

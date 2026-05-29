package dev.klerkframework.devmcp.codegenerator

import dev.klerkframework.devmcp.CodeSnippet

fun generateDataContainerSnippet(dataContainerName: String, type: ContainerType): CodeSnippet =
    when (type) {
        ContainerType.String -> CodeSnippet(
            code = """
                class $dataContainerName(value: String) : StringContainer(value) {
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
                class $dataContainerName(value: Int) : IntContainer(value) {
                    override val min = 0
                    override val max = Int.MAX_VALUE
                }
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.IntContainer"),
            instructions = instructions
        )

        ContainerType.Long -> CodeSnippet(
            code = """
                class $dataContainerName(value: Long) : LongContainer(value) {
                    override val min = 0L
                    override val max = Long.MAX_VALUE
                }
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.LongContainer"),
            instructions = instructions
        )

        ContainerType.Float -> CodeSnippet(
            code = """
                class $dataContainerName(value: Float) : FloatContainer(value) {
                    override val min = 0f
                    override val max = Float.MAX_VALUE
                }
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.FloatContainer"),
            instructions = instructions
        )

        ContainerType.Boolean -> CodeSnippet(
            code = """
                class $dataContainerName(value: Boolean) : BooleanContainer(value)
            """.trimIndent(),
            imports = listOf("dev.klerkframework.klerk.datatypes.BooleanContainer"),
            instructions = instructions
        )

        ContainerType.GeoPosition -> CodeSnippet(
            code = """
                class $dataContainerName(value: GeoPosition) : GeoPositionContainer(value)
            """.trimIndent(),
            imports = listOf(
                "dev.klerkframework.klerk.datatypes.GeoPositionContainer",
                "dev.klerkframework.klerk.misc.GeoPosition"
            ),
            instructions = instructions
        )

        ContainerType.Duration -> CodeSnippet(
            code = """
                class $dataContainerName(value: Duration) : DurationContainer(value)
            """.trimIndent(),
            imports = listOf(
                "dev.klerkframework.klerk.datatypes.DurationContainer",
                "kotlin.time.Duration"
            ),
            instructions = instructions
        )

        ContainerType.Instant -> CodeSnippet(
            code = """
                class $dataContainerName(value: Instant) : InstantContainer(value)
            """.trimIndent(),
            imports = listOf(
                "dev.klerkframework.klerk.datatypes.InstantContainer",
                "kotlin.time.Instant"
            ),
            instructions = instructions
        )

        ContainerType.ModelReference -> TODO()
    }

enum class ContainerType {
    String, Int, Long, Float, Boolean, GeoPosition, Duration, Instant, ModelReference,
}

private const val instructions =
    "Put this code in a file named 'DataContainers.kt' unless explicitly stated otherwise. Make sure the provided imports are in the file."


abstract class DataContainerType(val name: String, val nullable: Boolean) {
    abstract fun asClass(): String
    fun asProperty() = "val ${name.replaceFirstChar { it.lowercase() }}: ${name.replaceFirstChar { it.uppercase() }}${if (nullable) "?" else ""}},"
    abstract fun imports(): Set<String>

}

class StringContainerType(name: String, nullable: Boolean, val minLength: Int, val maxLength: Int, val maxLines: Int) : DataContainerType(name, nullable) {
    override fun asClass() = """
                class $name(value: String) : StringContainer(value) {
                    override val minLength = $minLength
                    override val maxLength = $maxLength
                    override val maxLines = $maxLines
                }
            """.trimIndent()

    override fun imports(): Set<String> = emptySet()
}


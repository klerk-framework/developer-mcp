package dev.klerkframework.devmcp.codegenerator

import dev.klerkframework.devmcp.CodeSnippet
import dev.klerkframework.devmcp.tool.PropertyDefinition
import kotlin.time.Duration
import kotlin.time.Instant

fun generateDataContainers(properties: Set<DataContainerType>): CodeSnippet {
    val imports = properties.flatMap { it.dataTypeImports() }.toSet()
    val code = properties.map { it.asClass() }.joinToString("\n\n")
    return CodeSnippet(code, imports.toList(), instructions)
}

enum class ContainerType {
    String, Int, Long, Float, Boolean, GeoPosition, Duration, Instant, ModelReference,
}

private const val instructions =
    "Put this code in a file named 'DataContainers.kt' unless explicitly stated otherwise. Make sure the provided imports are in the file."


abstract class DataContainerType(nameMaybeLowercase: String, val nullable: Boolean) {

    val name = nameMaybeLowercase.replaceFirstChar { it.titlecase() }

    abstract fun asClass(): String
    fun asProperty(): String {
        return if (this is ModelReferenceType) {
            "val ${name.replaceFirstChar { it.lowercase() }}: ModelID<${this.fkModel}>"
        } else {
            "val ${name.replaceFirstChar { it.lowercase() }}: ${name.replaceFirstChar { it.uppercase() }}${if (nullable) "?" else ""},"
        }
    }

    abstract fun dataTypeImports(): Set<String>
}

class StringContainerType(
    name: String,
    nullable: Boolean,
    val minLength: Int,
    val maxLength: Int,
    val maxLines: Int,
    val recommendedDefault: String?
) : DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd =
            if (recommendedDefault == null) "\n}" else "\n    override val recommendedDefault = \"$recommendedDefault\"\n}"
        return """
                class $name(value: String) : StringContainer(value) {
                    override val minLength = $minLength
                    override val maxLength = $maxLength
                    override val maxLines = $maxLines
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> = setOf("dev.klerkframework.klerk.datatypes.StringContainer")
}

class IntContainerType(name: String, nullable: Boolean, val min: Int, val max: Int, val recommendedDefault: String?) :
    DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd =
            if (recommendedDefault == null) "\n}" else "\n    override val recommendedDefault = $recommendedDefault\n}"
        return """
                class $name(value: Int) : IntContainer(value) {
                    override val min = $min
                    override val max = $max
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> = setOf("dev.klerkframework.klerk.datatypes.IntContainer")
}

class LongContainerType(name: String, nullable: Boolean, val min: Int, val max: Long, val recommendedDefault: String?) :
    DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd =
            if (recommendedDefault == null) "}" else "\n    override val recommendedDefault = $recommendedDefault\n}"
        return """
                class $name(value: Long) : LongContainer(value) {
                    override val min = $min
                    override val max = $max
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> = setOf("dev.klerkframework.klerk.datatypes.LongContainer")
}

class FloatContainerType(
    name: String,
    nullable: Boolean,
    val min: Float,
    val max: Float,
    val recommendedDefault: String?
) : DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd =
            if (recommendedDefault == null) "}" else "\n    override val recommendedDefault = ${recommendedDefault.toFloat()}\n}"
        return """
                class $name(value: Float) : FloatContainer(value) {
                    override val min = $min
                    override val max = $max
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> = setOf("dev.klerkframework.klerk.datatypes.FloatContainer")
}

class BooleanContainerType(name: String, nullable: Boolean, val recommendedDefault: String?) :
    DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd =
            if (recommendedDefault == null) "}" else "\n    override val recommendedDefault = ${recommendedDefault.toBoolean()}\n}"
        return """
                class $name(value: Boolean) : BooleanContainer(value) {
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> = setOf("dev.klerkframework.klerk.datatypes.BooleanContainer")
}

class DurationContainerType(
    name: String,
    nullable: Boolean,
    val min: Duration,
    val max: Duration?,
    val recommendedDefault: String?
) : DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd = if (recommendedDefault == null) "}" else "\n    override val recommendedDefault = ${
            Duration.parse(recommendedDefault).toIsoString()
        }\n}"
        return """
                class $name(value: Duration) : DurationContainer(value) {
                    override val min = $min
                    override val max = TODO()
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> =
        setOf("dev.klerkframework.klerk.datatypes.DurationContainer", "kotlin.time.Duration")
}

class InstantContainerType(
    name: String,
    nullable: Boolean,
    val min: Instant,
    val max: Instant?,
    val recommendedDefault: String?
) : DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd =
            if (recommendedDefault == null) "}" else "\n    override val recommendedDefault = TODO()}\n}"
        return """
                class $name(value: Instant) : InstantContainer(value) {
                    override val min = $min
                    override val max = TODO()
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> =
        setOf("dev.klerkframework.klerk.datatypes.InstantContainer", "kotlin.time.Instant")
}


class GeoPositionContainerType(name: String, nullable: Boolean, val recommendedDefault: String?) :
    DataContainerType(name, nullable) {
    override fun asClass(): String {
        val defaultAndEnd = if (recommendedDefault == null) "}" else "\n    override val recommendedDefault = TODO()\n}"
        return """
                class $name(value: GeoPosition) : GeoPositionContainer(value) {
                """.trimIndent() + defaultAndEnd
    }

    override fun dataTypeImports(): Set<String> =
        setOf("dev.klerkframework.klerk.datatypes.GeoPositionContainer", "dev.klerkframework.klerk.misc.GeoPosition")
}

class ModelReferenceType(name: String, nullable: Boolean, val fkModel: String) : DataContainerType(name, nullable) {
    override fun asClass(): String = ""
    override fun dataTypeImports(): Set<String> = setOf("dev.klerkframework.klerk.misc.ModelID")
}

fun toDataContainerType(pd: PropertyDefinition) =
    when (pd.type) {
        ContainerType.String -> StringContainerType(pd.name, pd.nullable, 0, 1000, 1, pd.defaultValue)
        ContainerType.Int -> IntContainerType(pd.name, pd.nullable, 0, Int.MAX_VALUE, pd.defaultValue)
        ContainerType.Long -> LongContainerType(pd.name, pd.nullable, 0, Long.MAX_VALUE, pd.defaultValue)
        ContainerType.Float -> FloatContainerType(pd.name, pd.nullable, 0f, Float.MAX_VALUE, pd.defaultValue)
        ContainerType.Boolean -> BooleanContainerType(pd.name, pd.nullable, pd.defaultValue)
        ContainerType.GeoPosition -> GeoPositionContainerType(pd.name, pd.nullable, pd.defaultValue)
        ContainerType.Duration -> DurationContainerType(pd.name, pd.nullable, Duration.ZERO, null, pd.defaultValue)
        ContainerType.Instant -> InstantContainerType(
            pd.name,
            pd.nullable,
            Instant.fromEpochSeconds(0),
            null,
            pd.defaultValue
        )

        ContainerType.ModelReference -> ModelReferenceType(pd.name, pd.nullable, requireNotNull(pd.fkModel))
    }

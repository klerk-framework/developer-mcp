package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.CodeSnippet
import dev.klerkframework.devmcp.codegenerator.ContainerType
import dev.klerkframework.devmcp.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*

const val generateModelFromTableSchema = "generate_model_from_table_schema"

private const val tableName = "table_name"
private const val columns = "columns"
/*
fun addToolGenerateModelFromTableSchema(mcpServer: Server) {
    mcpServer.addTool(
        name = generateModelFromTableSchema,
        description = "Generates code for a new model using input from an SQL schema",
        toolAnnotations = readOnly,
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put(tableName, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the SQL table that should be converted to a Klerk model")
                })
                put(columns, buildJsonObject {
                    put("type", "array")
                    put("description", "The columns of the SQL table")
                    put("items", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                            put("sql_type", buildJsonObject { put("type", "string") })
                            put("nullable", buildJsonObject { put("type", "boolean") })
                            put("max_length", buildJsonObject { put("type", "integer") })
                            put("fk_model", buildJsonObject { put("type", "string") })
                        })
                        put("required", buildJsonArray {
                            add("name")
                            add("sql_type")
                            add("nullable")
                        })
                    })
                })
            },
            required = listOf(tableName, columns)
        )
    ) { request ->
        val rawTableName = request.arguments?.get(tableName)?.jsonPrimitive?.content
            ?: error("No $tableName provided")
        val model = tableNameToModelName(rawTableName)

        val columnsArray = request.arguments?.get(columns)?.jsonArray
            ?: error("No $columns provided")

        val parsedColumns = columnsArray.map { element ->
            val obj = element.jsonObject
            PropertyDefinition(
                name = obj["name"]!!.jsonPrimitive.content,
                type = obj["sql_type"]!!.jsonPrimitive.content,
                nullable = obj["nullable"]!!.jsonPrimitive.boolean,
                maxLength = obj["max_length"]?.jsonPrimitive?.intOrNull,
                fkModel = obj["fk_model"]?.jsonPrimitive?.contentOrNull,
            )
        }

        val snippet = generateModelFromSchemaSnippet(model, parsedColumns)
        CallToolResult(content = listOf(TextContent(json.encodeToString(snippet))))
    }
}

data class PropertyDefinition(
    val name: String,
    val type: ContainerType,
    val nullable: Boolean,
    val fkModel: String?,
)

private fun tableNameToModelName(tableName: String): String =
    tableName.split("_", " ", "-")
        .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }


private fun sqlTypeToContainerClass(col: PropertyDefinition): Pair<String, String> {
    if (col.fkModel != null) {
        return "ModelReference<${col.fkModel}>" to "dev.klerkframework.klerk.ModelReference"
    }
    return when (col.type.uppercase().substringBefore("(")) {
        "VARCHAR", "TEXT", "CHAR", "NVARCHAR", "NCHAR", "CLOB" -> "StringContainer" to "dev.klerkframework.klerk.datatypes.StringContainer"
        "INT", "INTEGER", "SMALLINT", "TINYINT", "MEDIUMINT" -> "IntContainer" to "dev.klerkframework.klerk.datatypes.IntContainer"
        "BIGINT" -> "LongContainer" to "dev.klerkframework.klerk.datatypes.LongContainer"
        "FLOAT", "REAL" -> "FloatContainer" to "dev.klerkframework.klerk.datatypes.FloatContainer"
        "BOOLEAN", "BOOL", "BIT" -> "BooleanContainer" to "dev.klerkframework.klerk.datatypes.BooleanContainer"
        "DATE", "DATETIME", "TIMESTAMP" -> "InstantContainer" to "dev.klerkframework.klerk.datatypes.InstantContainer"
        else -> "StringContainer" to "dev.klerkframework.klerk.datatypes.StringContainer"
    }
}

private fun generateDataContainerCode(col: PropertyDefinition): Pair<String, List<String>> {
    val fieldName = col.name.replaceFirstChar { it.uppercaseChar() }
    if (col.fkModel != null) {
        return "" to emptyList()
    }
    val (containerClass, import) = sqlTypeToContainerClass(col)
    val isString = containerClass == "StringContainer"
    val maxLen = col.maxLength ?: 255
    val code = if (isString) {
        """
class $fieldName(value: String) : $containerClass(value) {
    override val minLength = ${if (col.nullable) 0 else 1}
    override val maxLength = $maxLen
    override val maxLines = 1
}""".trimIndent()
    } else {
        """
class $fieldName(value: ${containerClassToValueType(containerClass)}) : $containerClass(value)""".trimIndent()
    }
    return code to listOf(import)
}

private fun containerClassToValueType(containerClass: String): String = when (containerClass) {
    "IntContainer" -> "Int"
    "LongContainer" -> "Long"
    "FloatContainer" -> "Float"
    "BooleanContainer" -> "Boolean"
    "InstantContainer" -> "Instant"
    else -> "String"
}

fun generateModelFromSchemaSnippet(model: String, cols: List<PropertyDefinition>): CodeSnippet {
    val imports = mutableSetOf(
        "import dev.klerkframework.klerk.*",
        "import dev.klerkframework.klerk.statemachine.StateMachine",
        "import dev.klerkframework.klerk.statemachine.stateMachine",
    )

    // Generate data container classes for non-FK columns
    val containerClasses = StringBuilder()
    for (col in cols) {
        if (col.fkModel != null) continue
        val (code, colImports) = generateDataContainerCode(col)
        if (code.isNotEmpty()) {
            containerClasses.appendLine(code)
            containerClasses.appendLine()
            colImports.forEach { imports.add("import $it") }
        }
    }

    // Generate data container fields
    val dataContainerFields = cols.joinToString("\n    ") { col ->
        val fieldName = col.name.replaceFirstChar { it.lowercaseChar() }
        val typeName = if (col.fkModel != null) {
            imports.add("import dev.klerkframework.klerk.ModelReference")
            val ref = "ModelReference<${col.fkModel}>"
            if (col.nullable) "$ref?" else ref
        } else {
            val containerClassName = col.name.replaceFirstChar { it.uppercaseChar() }
            if (col.nullable) "$containerClassName?" else containerClassName
        }
        "val $fieldName: $typeName,"
    }

    val code = buildString {
        if (containerClasses.isNotEmpty()) {
            append(containerClasses.toString().trimEnd())
            appendLine()
            appendLine()
        }
        append("""

data class $model(
    $dataContainerFields
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

object Create${model} : VoidEventWithParameters<${model}, $model>(${model}::class, EventVisibility.CODE, $model::class)

object Update${model} : InstanceEventWithParameters<${model}, $model>(${model}::class, EventVisibility.CODE, $model::class)

object Delete${model} : InstanceEventNoParameters<${model}>(${model}::class, EventVisibility.CODE)

fun create${model}(args: ArgForVoidEvent<${model}, $model, Ctx, Collections>): ${model} {
    return ${model}(args.command.params)
}

fun update${model}(args: ArgForInstanceEvent<${model}, $model, Ctx, Collections>): ${model} {
    return ${model}(args.command.params)
}
        """.trimIndent())
    }

    return CodeSnippet(
        code = code,
        imports = imports.sorted(),
        instructions = "Put this code in a file named '$model.kt' in the package 'models'. Make sure the provided imports are in the file."
    )
}


 */
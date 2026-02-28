package com.example.tool

import com.example.CodeSnippet
import com.example.capFirstLetter
import com.example.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private const val FUNCTION_NAME = "function_name"

private const val IN_VOID_STATE = "in_void_state"

private const val MODEL_NAME = "model_name"

private const val PARAMETER_CLASS_NAME = "parameter_class_name"

private const val HAS_PARAMETERS = "has_parameters"



fun addToolGenerateFunctionCreateModel(mcpServer: Server) {
    mcpServer.addTool(
        name = "generate_function_create_model",
        description = "Returns a function with a signature suitable to use in createModel within voidState in a state machine. ",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
/*                put(FUNCTION_NAME, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the function that should be created.")
                })
                put(IN_VOID_STATE, buildJsonObject {
                    put("type", "boolean")
                    put("description", "true if this is used in a voidState block, false if it is used in a state block.")
                })

 */
                put(MODEL_NAME, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the model that the state machine is for.")
                })

                put(HAS_PARAMETERS, buildJsonObject {
                    put("type", "boolean")
                    put("description", "true if the event has parameters, false if it does not.")
                })

                /*put(PARAMETER_CLASS_NAME, buildJsonObject {
                    put("type", "string")
                    put("description", "The name of the class that contains the parameters for the event. Can be empty if no parameters are needed.")
                })

                 */
            },
          //  required = listOf(FUNCTION_NAME, IN_VOID_STATE, MODEL_NAME, PARAMETER_CLASS_NAME)
            required = listOf(MODEL_NAME, HAS_PARAMETERS)
        )
    ) { request ->
        val modelName = request.arguments?.get(MODEL_NAME)?.jsonPrimitive?.content ?: ""
        val hasParameters = request.arguments?.get(HAS_PARAMETERS)?.jsonPrimitive?.booleanOrNull ?: false
/*        val functionName = request.arguments?.get(FUNCTION_NAME)?.jsonPrimitive?.content ?: ""
        val inVoidState = request.arguments?.get(IN_VOID_STATE)?.jsonPrimitive?.booleanOrNull ?: false
        val parameterClassName = request.arguments?.get(PARAMETER_CLASS_NAME)?.jsonPrimitive?.content ?: ""
 */
        //generateFunctionCreateModel(modelName, parameterClassName, inVoidState, functionName)
        generateFunctionCreateModel(modelName, hasParameters)
    }
}

fun generateFunctionCreateModel(
    maybeLowerCaseModelName: String,
    hasParameters: Boolean,
//    parameterClassName: String,
  //  inVoidState: Boolean,
    //functionName: String
): CallToolResult {
    val modelName = capFirstLetter(maybeLowerCaseModelName)
    val parameterClass = if (hasParameters) {
        capFirstLetter("Create${modelName}Parameters")
    } else {
        "Nothing?"
    }

    val snippet =
        CodeSnippet(
            code = "fun create${modelName}(args: ArgForVoidEvent<$modelName, $parameterClass, Ctx, Views>): $modelName {\n" +
                    "   TODO()\n" +
                    "}\n",
            imports = listOf("dev.klerkframework.klerk.*"),
            instructions = "Paste the code into the current file."
        )

    return CallToolResult(content = listOf(TextContent(json.encodeToString(snippet))))
}

/*
"Use this when the user wants to ..."
                "Do NOT use this tool if"
 */
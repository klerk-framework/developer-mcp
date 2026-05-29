package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.codegenerator.StringContainerType
import dev.klerkframework.devmcp.codegenerator.generateWholeModel
import kotlin.test.Test

class GenerateModelFromSchemaSnippetTest {

    @Test
    fun generateModelFromSchemaSnippet() {
        val snippet = generateWholeModel(
            "Book", listOf(
                StringContainerType("Title", false, 1, 100, 1),
                StringContainerType("SubTitle", false, 1, 100, 1),
            )
        )
        println(snippet)
    }

}
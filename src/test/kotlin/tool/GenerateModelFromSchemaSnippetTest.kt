package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.codegenerator.*
import kotlin.test.Test

class GenerateModelFromSchemaSnippetTest {

    @Test
    fun generateModelFromSchemaSnippet() {
        val properties = setOf(
            IntContainerType("pages", false, 1, 100, "1"),
            StringContainerType("Title", false, 1, 100, 1, "the sequel"),
            ModelReferenceType("Author", false, "Author"),
        )
        val snippet = generateWholeModel("Book", properties)
        println(snippet)

        println(generateDataContainers(properties).code)
    }

}

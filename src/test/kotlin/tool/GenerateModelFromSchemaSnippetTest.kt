package dev.klerkframework.devmcp.tool

import dev.klerkframework.devmcp.codegenerator.IntContainerType
import dev.klerkframework.devmcp.codegenerator.ModelReferenceType
import dev.klerkframework.devmcp.codegenerator.StringContainerType
import dev.klerkframework.devmcp.codegenerator.generateDataContainers
import dev.klerkframework.devmcp.codegenerator.generateWholeModel
import kotlin.test.Test

class GenerateModelFromSchemaSnippetTest {

    @Test
    fun generateModelFromSchemaSnippet() {
        val properties = setOf(
            IntContainerType("Pages", false, 1, 100, "1"),
            StringContainerType("Title", false, 1, 100, 1, "the sequel"),
            ModelReferenceType("Author", false, "Author"),
        )
        val snippet = generateWholeModel("Book", properties)
        println(snippet)

        println(generateDataContainers(properties).code)
    }

}

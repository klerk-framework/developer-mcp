package dev.klerkframework.devmcp.tool

import kotlin.test.Test


class GenerateFunctionCreateModelsTest {

    @Test
    fun generateFunctionCreateModel() {
        val result = generateFunctionCreateModel("Book", true)
        println(result)
    }

}
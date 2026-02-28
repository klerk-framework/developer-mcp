package com.example.tool

import kotlin.test.Test


class GenerateFunctionCreateModelTest {

    @Test
    fun generateFunctionCreateModel() {
        val result = generateFunctionCreateModel("Book", true)
        println(result)
    }

}
package dev.klerkframework.devmcp.codegenerator

fun generateWholeModel(model: String, properties: List<DataContainerType>): String {
    return """
            data class ${model}(
                ${properties.map { it.asProperty() }.joinToString(",\n") { it.trimIndent() }}
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
            
            object Create${model} : VoidEventWithParameters<${model}, ${model}>(${model}::class, EventVisibility.CODE, ${model}::class)
            
            object Update${model} : InstanceEventWithParameters<${model}, ${model}>(${model}::class, EventVisibility.CODE, ${model}::class)
            
            object Delete${model} : InstanceEventNoParameters<${model}>(${model}::class, EventVisibility.CODE)
            
            fun create${model}(args: ArgForVoidEvent<${model}, ${model}, Ctx, Collections>): ${model} {
                return args.command.params
            }
            
            fun update${model}(args: ArgForInstanceEvent<${model}, ${model}, Ctx, Collections>): ${model} {
                return args.command.params
            }"""
}
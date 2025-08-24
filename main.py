from typing import Any
import httpx
from mcp.server.fastmcp import FastMCP
from pydantic import BaseModel, Field

from create_event import generate_event_response
from utils import cap_first_letter, CodeSnippet

# Initialize FastMCP server
mcp = FastMCP("klerk-dev-mcp")


@mcp.tool()
async def generate_function_create_model(function_name: str, in_void_state: bool, model_name: str,
                                         parameter_class_name: str) -> CodeSnippet:
    """Returns a function with a signature suitable to use in createModel within a state machine.

    Args:
        function_name: The name of the function that should be created.
        in_void_state: true if this is used in a voidState block, false if it is used in a state block.
        model_name: The name of the model that the state machine is for.
        parameter_class_name: The name of the class that contains the parameters for the event. Can be empty if no parameters are needed.
    """
    model_name = cap_first_letter(model_name)
    if parameter_class_name == "":
        parameter_class_name = "Nothing?"
    else:
        parameter_class_name = cap_first_letter(parameter_class_name)

    if in_void_state:
        return CodeSnippet(
            code=(f'fun {function_name}(args: ArgForVoidEvent<{model_name}, {parameter_class_name}, Ctx, Views>): {model_name.capitalize()} {{\n'
                  f'   TODO()\n'
                  f'}}\n'),
            imports=['dev.klerkframework.klerk.*'],
            instructions='Paste the code into klerk/functions.kt. Leave the TODO() as-is.'
        )

    return CodeSnippet(
        code=(f'fun {function_name}(args: ArgForInstanceEvent<{model_name}, {parameter_class_name}, Ctx, Views>): {model_name.capitalize()} {{\n'
              f'   TODO()\n'
              f'}}\n'),
        imports=['dev.klerkframework.klerk.*'],
        instructions='Paste the code into klerk/functions.kt. Leave the TODO() as-is. Create an import from the file where function reference is located.'
    )


@mcp.tool()
async def generate_initial_configuration() -> CodeSnippet:
    """Returns Kotlin code that can create a Klerk configuration."""
    return CodeSnippet(
        code='''fun createConfig(): Config<Ctx, Views> {
    val views = Views()
    return ConfigBuilder<Ctx, Views>(views).build {
        persistence(RamStorage())
        managedModels {
        }
        authorization {
            insecureAllowEverything()
        }
        contextProvider { actor -> Ctx(actor) }
    }
}

data class Ctx(
    override val actor: ActorIdentity,
    override val auditExtra: String? = null,
    override val translation: Translation = DefaultTranslation,
    override val time: Instant = Clock.System.now()
) : KlerkContext

class Views()
''',
        imports=['import dev.klerkframework.klerk.*', 'import dev.klerkframework.klerk.storage.RamStorage',
                 'import kotlinx.datetime.Clock', 'import kotlinx.datetime.Instant',
                 'import dev.klerkframework.klerk.collection.*'],
        instructions='Paste the code into klerk/config.kt'
    )


@mcp.tool()
async def generate_model(model_name: str, properties: list[str]) -> str:
    """Returns instructions for creating a model.

    Args:
        model_name: The name of the model
    """
    model_name = cap_first_letter(model_name)
    if properties.count == 0:
        properties = ["something"]

    return (f'1. Create a package called {model_name}\n'
            f'2. Create a data class called {model_name} in the package {model_name.lower()}. The class should have the following properties: '
            f'{properties}. The type of each property should be a class with the same name\n'
            f'3. Create the classes you need for the properties. The classes should inherit from StringContainer.\n'
            f'4. Use the tool "create_statemachine" to create a function that returns a StateMachine for the model.\n'
            f'5. Create a new property in the class Views:'
            f'```kotlin\n'
            f'class Views {{'
            f'  val {model_name.lower()}: ModelCollections<{model_name}, Ctx> = ModelCollections()\n'
            f'  // other properties\n'
            f'}}\n'
            f'6. Add the model to the list of managed models in the config by adding a line in the managedModels block.\n'
            f'The line should be "model({model_name}.class, create{model_name}StateMachine(), views.{model_name})"\n'
            )


@mcp.tool()
async def generate_statemachine(model_name: str) -> CodeSnippet:
    """Returns Kotlin code that can create a state machine.

    Args:
        model_name: The name of the model
    """
    cap_name = cap_first_letter(model_name)
    return CodeSnippet(
        code=f'''enum class {cap_name}States {{ Created }}

fun create{cap_name}StateMachine(): StateMachine<{cap_name}, {cap_name}States, Ctx, Views> = stateMachine {{

    voidState {{
        TODO("Add onEvent here")
    }}
    
    state({cap_name}States.Created) {{
    }}

}}
''',
        imports=['import dev.klerkframework.klerk.*', 'import dev.klerkframework.klerk.statemachine.*',
                 'dev.klerkframework.klerk.datatypes.*'],
        instructions=f'''Paste the code into klerk/{model_name.lower()}/statemachine.kt
Leave the TODO() as-is.
If there already exists a state machine
for the model, try to use the existing state machine and add parts from the new state machine to the existing one.
If there is no existing state machine, use the code provided by this tool as-is.''')


@mcp.tool()
async def generate_event(event_name: str, model_name: str, is_void_event: bool, has_params: bool) -> list[CodeSnippet]:
    """Returns Kotlin code that adds a new event to a state machine."""
    return generate_event_response(event_name, model_name, is_void_event, has_params)



if __name__ == "__main__":
    mcp.run(transport='stdio')

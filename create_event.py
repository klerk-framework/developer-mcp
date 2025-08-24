from utils import cap_first_letter, CodeSnippet
from mcp.server.fastmcp import Context

def generate_event_response(event_name: str, model_name: str, is_void_event: bool, has_params: bool) -> list[CodeSnippet]:
    model_name = cap_first_letter(model_name)
    event_name = cap_first_letter(event_name)

    event_declaration = CodeSnippet(
        code = f'event({event_name}) {{}}',
        imports=[],
        instructions = f'Add this line at the top of the statemachine block for {model_name}.'
    )

    if is_void_event and not has_params:
        return [CodeSnippet(
            code=(f'''object {event_name} : VoidEventNoParameters<{model_name}>(
    forModel = {model_name.capitalize()}::class,
    isExternal = true
)
'''), imports=[],
            instructions=f'Paste this code into the same file as the state machine for {model_name}.'),
        event_declaration,
        CodeSnippet(
            code = f'''onEvent({event_name}) {{
    createModel({model_name}.Created, ::new{model_name})
}}''',
            imports=[],
            instructions = f'''Add this to the void event block for {model_name}. There will be a reference to a missing function, fix it using generate_function_create_model with these parameters:
function_name=new{model_name}'
in_void_state=true
model_name={model_name}
parameter_class_name=""'''
        )]

    if not is_void_event and not has_params:
        return [CodeSnippet(
            code=(f'''object {event_name} : InstanceEventNoParameters<{model_name}>(
    forModel = {model_name}::class,
    isExternal = true
)
'''), imports=[],
            instructions=f'Paste this code into the same file as the state machine for {model_name}.'),
        event_declaration]

    # has_params == True
    params_class = f'{event_name}Params'

    if is_void_event:
        return [CodeSnippet(
            code=(
                f'''object {event_name} : VoidEventWithParameters<{model_name}, {params_class}>(
    forModel = {model_name}::class,
    isExternal = true,
    parametersClass = {params_class}::class
)

data class {params_class}(
    val exampleProperty: KlerkExampleDataContainer,
)
'''), imports=[],
            instructions=f'Paste this code into the same file as the state machine for {model_name}.'),
            event_declaration]

    return [CodeSnippet(
        code=(
            f'''object {event_name} : InstanceEventWithParameters<{model_name}, {params_class}>(
    forModel = {model_name}::class,
    isExternal = true,
    parametersClass = {params_class}::class
)

data class {params_class}(
    val exampleProperty: KlerkExampleDataContainer,
)
'''), imports=[],
        instructions=f'Paste this code into the same file as the state machine for {model_name}.'),
        event_declaration]

from mcp.server.fastmcp import FastMCP
from pydantic import BaseModel, Field

class CodeSnippet(BaseModel):
    """Generated code suitable for use in Klerk."""

    code: str = Field(description="Code that should be pasted into a Klerk project")
    imports: list[str] = Field(description="Import statements that are needed by the code")
    instructions: str = Field(description="Instructions for using the code")

def cap_first_letter(s: str) -> str:
    return s[0].upper() + s[1:]

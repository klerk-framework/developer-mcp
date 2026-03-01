# Klerk MCP

This is an MCP that improves the developer experience when using AI to build systems with the Klerk framework. 

## Build
```bash
./gradlew build
```

## How to use

The MCP server can be started in two ways. STDIO is the recommended way.

### STDIO

In IntellJ -> Settings -> Junie -> MCP settings:

```json
{
  "mcpServers": {
    "klerk-mcp": {
      "command": "/usr/bin/java",
      "args": [
        "-jar",
        "/path/to/klerk-mcp-kt/build/libs/klerk-mcp-kt-all.jar"
      ]
    }
  }
}
```

### HTTP SSE
This mode is preferred when developing this MCP server itself. This allows you to log and set breakpoints to see how the MCP is used.

Set the environment variable `KLERK_MCP_PORT` to the port you want to use (3000 in this case) and start the server.

In IntellJ -> Settings -> Junie -> MCP settings:

```json
{
  "mcpServers": {
    "klerk-mcp": {
      "type": "sse",
      "url": "http://localhost:3000/",
      "note": "For SSE connections, add this URL directly in your MCP Client"
    }
  }
}
```

# AI Agent Configuration

This directory contains shared configuration and instructions for AI agents working on the Docker-WSL-Manager project.

## Structure

- **INSTRUCTIONS.md**: Common instructions and best practices that apply to all AI agents
- **mcp-servers.json**: Common MCP (Model Context Protocol) server configurations (single source of truth)
- **validate-mcp-sync.sh**: Validation script to ensure all agent MCP configurations are in sync

## Agent-Specific Configurations

Different AI agents have their own configuration directories that reference the shared files:

- **`.gemini/`**: Configuration for Google Gemini CLI
- **`.github/copilot/`**: Configuration for GitHub Copilot
- **`.cursor/`**: Configuration for Cursor AI

Each agent-specific directory contains:
- An instructions file that references the common INSTRUCTIONS.md
- A settings.json file that contains the MCP server configuration (copied from mcp-servers.json)

## DRY Principle

To follow the DRY (Don't Repeat Yourself) principle:

1. **Common instructions** are maintained in `.ai/INSTRUCTIONS.md`
   - Agent instructions use markdown links to reference this file
   - Changes automatically apply to all agents

2. **Common MCP server configurations** are maintained in `.ai/mcp-servers.json`
   - This is the single source of truth
   - Content is copied to each agent's settings.json
   - Use `validate-mcp-sync.sh` to verify all configurations are in sync

3. **Agent-specific instructions** are kept minimal and only include agent-specific details

This approach ensures:
- Easy maintenance: Update common instructions in one place
- Consistency: All agents follow the same guidelines
- Extensibility: Easy to add new AI agents by creating a new directory with references to shared files
- Validation: Script to catch configuration drift

## Validation

To verify all agent MCP configurations are in sync with the canonical configuration:

```bash
./.ai/validate-mcp-sync.sh
```

This script will:
- Compare each agent's MCP server configuration with the canonical `.ai/mcp-servers.json`
- Report any differences
- Exit with error code if configurations are out of sync

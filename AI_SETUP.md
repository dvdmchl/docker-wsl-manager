# AI Agent Setup Guide

This document explains how the Docker-WSL-Manager project is configured for multiple AI agents to contribute effectively.

## Overview

The project follows the **DRY (Don't Repeat Yourself)** principle for AI agent configurations. Common instructions and settings are maintained in a single location and referenced by agent-specific configurations.

## Directory Structure

```
docker-wsl-manager/
├── .ai/                          # Shared AI agent configuration
│   ├── README.md                 # Overview of AI configuration
│   ├── INSTRUCTIONS.md           # Common instructions for all AI agents
│   └── mcp-servers.json          # Common MCP server configurations
├── .gemini/                      # Google Gemini CLI configuration
│   ├── GEMINI.md                 # Gemini-specific instructions (references .ai/INSTRUCTIONS.md)
│   └── settings.json             # Gemini settings (references .ai/mcp-servers.json)
├── .github/copilot/              # GitHub Copilot configuration
│   ├── instructions.md           # Copilot-specific instructions (references .ai/INSTRUCTIONS.md)
│   └── settings.json             # Copilot settings (references .ai/mcp-servers.json)
└── .cursor/                      # Cursor AI configuration
    ├── instructions.md           # Cursor-specific instructions (references .ai/INSTRUCTIONS.md)
    └── settings.json             # Cursor settings (references .ai/mcp-servers.json)
```

## How It Works

### 1. Common Instructions (.ai/INSTRUCTIONS.md)

All project-specific development instructions, coding standards, build commands, and best practices are maintained in a single file: `.ai/INSTRUCTIONS.md`

This file includes:
- Project overview and tech stack
- Development setup and prerequisites
- Build, run, and test commands
- Tool execution rules (e.g., WSL prefix for Docker commands)
- Code quality requirements (Sonar, Checkstyle, SpotBugs)
- Testing policies
- Best practices for agents

### 2. Common MCP Servers (.ai/mcp-servers.json)

MCP (Model Context Protocol) server configurations that are common across all AI agents are defined in `.ai/mcp-servers.json`

This file includes:
- GitHub MCP server configuration
- Any other shared MCP servers

**Note**: Since JSON doesn't support file inclusion natively, each agent's `settings.json` contains a copy of the common MCP servers with a comment referencing the source. When updating MCP servers:
1. Update the common file `.ai/mcp-servers.json` first
2. Copy the updated content to each agent's `settings.json` file
3. This maintains a single source of truth while ensuring compatibility with JSON parsers

### 3. Agent-Specific Configurations

Each AI agent has its own directory with two files:

#### Instructions File
- References the common instructions
- Contains only agent-specific instructions (if any)
- Keeps duplication minimal

#### Settings File
- References the common MCP servers configuration
- Can add agent-specific MCP servers if needed

## Supported AI Agents

### Currently Configured

1. **Google Gemini CLI** (`.gemini/`)
   - Original configuration updated to reference shared files
   
2. **GitHub Copilot** (`.github/copilot/`)
   - New configuration following the DRY principle
   
3. **Cursor AI** (`.cursor/`)
   - New configuration following the DRY principle

### Adding New AI Agents

To add support for a new AI agent:

1. Create a new directory for the agent (e.g., `.cline/`, `.codex/`)
2. Create an instructions file that references `.ai/INSTRUCTIONS.md`
3. Create a settings file that references `.ai/mcp-servers.json`
4. Add agent-specific instructions only if needed

Example for a hypothetical "Cline" agent:

```bash
mkdir .cline
```

Create `.cline/instructions.md`:
```markdown
# Cline Project Guide

## Common Instructions
For general project information, please refer to: [Common AI Agent Instructions](../.ai/INSTRUCTIONS.md)

## Cline-Specific Instructions
*(Add only Cline-specific instructions here)*
```

Create `.cline/settings.json`:
```json
{
  "$schema": "https://json-schema.org/draft-07/schema",
  "mcpServers": {
    "$comment": "Common MCP servers are defined in ../.ai/mcp-servers.json. Copy the content from that file here.",
    "github": {
      "httpUrl": "https://api.githubcopilot.com/mcp/",
      "headers": {
        "Authorization": "Bearer $GITHUB_MCP_PAT"
      }
    }
  }
}
```

## Maintenance

### Updating Common Instructions

To update instructions that apply to all agents:
1. Edit `.ai/INSTRUCTIONS.md`
2. All agents automatically benefit from the update

### Updating MCP Server Configuration

To update MCP servers for all agents:
1. Edit `.ai/mcp-servers.json` with the new configuration
2. Copy the updated `mcpServers` content to each agent's `settings.json` file:
   - `.gemini/settings.json`
   - `.github/copilot/settings.json`
   - `.cursor/settings.json`
   - Any other agent-specific settings files
3. Run the validation script to ensure all configurations are in sync:
   ```bash
   ./.ai/validate-mcp-sync.sh
   ```

**Note**: JSON doesn't support automatic file inclusion, so the content needs to be copied manually. However, having a single source file (`.ai/mcp-servers.json`) ensures consistency and makes it clear what the canonical configuration should be. The validation script helps catch any synchronization issues.

### Adding Agent-Specific Instructions

If an agent needs specific instructions:
1. Edit the agent's instructions file (e.g., `.gemini/GEMINI.md`)
2. Add only agent-specific content
3. Keep the reference to common instructions

## Benefits

1. **Single Source of Truth**: Common instructions maintained in one place
2. **Easy Maintenance**: Update once, apply everywhere
3. **Consistency**: All agents follow the same guidelines
4. **Extensibility**: Easy to add new AI agents
5. **Clarity**: Clear separation between common and agent-specific instructions

## References

- [Common AI Agent Instructions](.ai/INSTRUCTIONS.md)
- [AI Configuration Overview](.ai/README.md)
- [Gemini Configuration](.gemini/GEMINI.md)
- [GitHub Copilot Configuration](.github/copilot/instructions.md)
- [Cursor AI Configuration](.cursor/instructions.md)

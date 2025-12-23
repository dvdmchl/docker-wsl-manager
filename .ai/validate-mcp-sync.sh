#!/bin/bash

# Validation script to ensure all agent MCP server configurations are in sync
# with the canonical .ai/mcp-servers.json file

set -e

CANONICAL_FILE=".ai/mcp-servers.json"
AGENT_FILES=(
  ".gemini/settings.json"
  ".github/copilot/settings.json"
  ".cursor/settings.json"
)

echo "=== MCP Server Configuration Validation ==="
echo ""

# Check if canonical file exists
if [ ! -f "$CANONICAL_FILE" ]; then
  echo "❌ Error: Canonical file $CANONICAL_FILE not found!"
  exit 1
fi

# Extract the mcpServers section from canonical file
CANONICAL_CONTENT=$(jq '.mcpServers' "$CANONICAL_FILE")

echo "Canonical MCP Servers configuration:"
echo "$CANONICAL_CONTENT"
echo ""
echo "---"
echo ""

# Check each agent file
ALL_MATCH=true
for agent_file in "${AGENT_FILES[@]}"; do
  if [ ! -f "$agent_file" ]; then
    echo "⚠️  Warning: $agent_file not found (skipping)"
    continue
  fi
  
  # Extract mcpServers section from agent file (excluding $comment fields)
  AGENT_CONTENT=$(jq 'del(.mcpServers["$comment"]) | .mcpServers' "$agent_file")
  
  # Compare configurations
  if [ "$CANONICAL_CONTENT" = "$AGENT_CONTENT" ]; then
    echo "✅ $agent_file is in sync"
  else
    echo "❌ $agent_file is OUT OF SYNC!"
    echo "   Expected:"
    echo "   $CANONICAL_CONTENT"
    echo "   Found:"
    echo "   $AGENT_CONTENT"
    ALL_MATCH=false
  fi
done

echo ""
if [ "$ALL_MATCH" = true ]; then
  echo "=== ✅ All MCP server configurations are in sync ==="
  exit 0
else
  echo "=== ❌ Some configurations are out of sync ==="
  echo ""
  echo "To fix:"
  echo "1. Review the differences above"
  echo "2. Copy the mcpServers content from $CANONICAL_FILE"
  echo "3. Update each out-of-sync agent settings.json file"
  exit 1
fi

#!/usr/bin/env bash

# Validation script to ensure all agent MCP server configurations are in sync
# with the canonical .ai/mcp-servers.json file

set -euo pipefail

# Check if jq is installed
if ! command -v jq &> /dev/null; then
  echo "❌ Error: jq is not installed. Please install jq to use this script."
  echo "   On Ubuntu/Debian: sudo apt-get install jq"
  echo "   On macOS: brew install jq"
  echo "   On Windows: choco install jq or download from https://stedolan.github.io/jq/"
  exit 1
fi

# jq filter to extract mcpServers while excluding all $comment fields
readonly JQ_FILTER='walk(if type == "object" then del(.["$comment"]) else . end) | .mcpServers'

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

# Extract the mcpServers section from canonical file (recursively exclude all $comment fields)
if ! CANONICAL_CONTENT=$(jq "$JQ_FILTER" "$CANONICAL_FILE" 2>&1); then
  echo "❌ Error: Failed to parse $CANONICAL_FILE"
  echo "   Details: $CANONICAL_CONTENT"
  exit 1
fi

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
  
  # Extract mcpServers section from agent file (recursively exclude all $comment fields)
  if ! AGENT_CONTENT=$(jq "$JQ_FILTER" "$agent_file" 2>&1); then
    echo "❌ $agent_file has invalid JSON format"
    echo "   Details: $AGENT_CONTENT"
    ALL_MATCH=false
    continue
  fi
  
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

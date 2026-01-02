# CLAUDE.md - AI Assistant Context

This file provides context for Claude Code sessions working on bb-mcp.

## Project Overview

bb-mcp is a lightweight MCP server written in Babashka (Clojure) that bridges Claude Code to Emacs via nREPL. It connects to emacs-mcp on port 7910 and provides 89 tools: 6 native + 83 Emacs tools.

## Key Files

### Core
| File | Purpose |
|------|---------|
| `src/bb_mcp/core.clj` | Main entry, MCP message loop, tool registry |
| `src/bb_mcp/protocol.clj` | JSON-RPC protocol (stdio communication) |
| `src/bb_mcp/nrepl_spawn.clj` | Auto-spawn emacs-mcp if not running |

### Native Tools
| File | Purpose |
|------|---------|
| `src/bb_mcp/tools/bash.clj` | Shell command execution |
| `src/bb_mcp/tools/file.clj` | File read/write/glob operations |
| `src/bb_mcp/tools/grep.clj` | Ripgrep wrapper |
| `src/bb_mcp/tools/nrepl.clj` | nREPL client with bencode (byte-based) |

### Emacs Tool Domains
| File | Domain |
|------|--------|
| `tools/emacs.clj` | Aggregator - combines all domain modules |
| `tools/emacs/core.clj` | Shared helpers (emacs-eval, wrap-emacs-call) |
| `tools/emacs/buffer.clj` | Buffer ops, elisp eval (19 tools) |
| `tools/emacs/magit.clj` | Git via Magit (10 tools) |
| `tools/emacs/memory.clj` | Project memory CRUD (12 tools) |
| `tools/emacs/swarm.clj` | Swarm agent orchestration (11 tools) |
| `tools/emacs/kanban.clj` | Kanban management (8 tools) |
| `tools/emacs/cider.clj` | CIDER REPL integration (8 tools) |
| `tools/emacs/projectile.clj` | Project navigation (6 tools) |
| `tools/emacs/org.clj` | Org-mode operations (4 tools) |
| `tools/emacs/prompt.clj` | Prompt capture/search (4 tools) |
| `tools/emacs/context.clj` | Full context aggregation (1 tool) |
| `tools/emacs/dynamic.clj` | Dynamic tool loading from emacs-mcp |

## Common Commands

```bash
# Run MCP server
bb mcp

# Run tests
bb test

# Start directly
bb -m bb-mcp.core
```

## Architecture Notes

1. **Tool Registration**: Tools are registered in `core.clj` by concatenating native tools + `emacs/tools`
2. **Emacs Tool Pattern**: Each domain module exports a `tools` vector with `{:spec ... :handler ...}` maps
3. **nREPL Delegation**: Emacs tools call `emacs-eval` which sends code to port 7910 via bencode
4. **Bencode**: Uses byte-based encoding for proper UTF-8 (not character-based)
5. **Dynamic Loading**: Can fetch tool specs from emacs-mcp at startup via `dynamic.clj`

## Adding Tools

1. Create spec with `:name`, `:description`, `:schema`
2. Create handler function taking args map
3. Add `{:spec ... :handler ...}` to domain module's `tools` vector
4. If new domain, require it in `tools/emacs.clj` and concat to `static-tools`

## Testing

Tests are in `test/bb_mcp/`. Run with `bb test`.

---

# Session Log

## Session: [DATE]

### Context
- **Goal**: [Brief description of what you're working on]
- **Branch**: [Current git branch if applicable]

### Progress
- [ ] Task 1
- [ ] Task 2

### Notes
[Any observations, decisions, or things to remember]

### Files Modified
- `path/to/file.clj` - [what changed]

---

## Previous Sessions

### Session: 2026-01-02
- Refactored emacs tools into DDD bounded context modules
- Fixed bencode to use byte-based I/O for UTF-8
- Added dynamic tool loading from emacs-mcp
- Fixed magit_status directory parameter handling
- Aligned swarm_collect timeout with emacs-mcp polling

# CLAUDE.md - AI Assistant Context

This file provides context for Claude Code sessions working on bb-mcp.

## Project Overview

bb-mcp is a lightweight MCP server written in Babashka (Clojure) that bridges Claude Code to Emacs via nREPL. It's a **multiplexer** - multiple bb-mcp instances share ONE hive-mcp JVM, reducing memory from ~500MB/instance to ~50MB/instance.

**Tool count**: 6 native + 108 dynamic (from hive-mcp) = 114 tools

## Key Files

### Core
| File | Purpose |
|------|---------|
| `src/bb_mcp/core.clj` | Main entry, MCP message loop, tool registry |
| `src/bb_mcp/protocol.clj` | JSON-RPC protocol (stdio communication) |
| `src/bb_mcp/nrepl_spawn.clj` | Auto-spawn hive-mcp if not running |

### Native Tools
| File | Purpose |
|------|---------|
| `src/bb_mcp/tools/bash.clj` | Shell command execution |
| `src/bb_mcp/tools/file.clj` | File read/write/glob operations |
| `src/bb_mcp/tools/grep.clj` | Ripgrep wrapper |
| `src/bb_mcp/tools/nrepl.clj` | nREPL client with bencode (byte-based) |

### Dynamic Tool Loading
| File | Purpose |
|------|---------|
| `tools/emacs.clj` | Facade for dynamic tools, exposes `init!` and `get-tools` |
| `tools/emacs/dynamic.clj` | Fetches tools from hive-mcp via nREPL at startup |

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

1. **Multiplexer Pattern**: Many bb-mcp instances (lightweight) share one hive-mcp JVM (heavyweight)
2. **Dynamic Loading**: At startup, `emacs/init!` queries hive-mcp for all tools via nREPL
3. **Forwarding Handlers**: Each dynamic tool gets a handler that forwards calls to hive-mcp
4. **No Static Tools**: All Emacs tools come from hive-mcp - add tools there, not here
5. **Bencode**: Uses byte-based encoding for proper UTF-8 (not character-based)

## Adding Tools

**Native tools** (run in Babashka):
1. Add to `src/bb_mcp/tools/` (bash, file, grep, nrepl)
2. Register in `core.clj` native-tools vector

**Emacs tools**: Add to hive-mcp - bb-mcp picks them up automatically via dynamic loading.

## Testing

Tests are in `test/bb_mcp/`. Run with `bb test`.

Unit tests work without hive-mcp. Integration tests skip gracefully if hive-mcp is unavailable.

---

# Session Log

## Session: 2026-01-03

### Context
- **Goal**: Achieve tool parity with hive-mcp, remove static tool maintenance burden

### Progress
- [x] Enable dynamic tool loading at startup (89 → 114 tools)
- [x] Remove all static tool modules (buffer, magit, memory, etc.)
- [x] Simplify emacs.clj to dynamic-only
- [x] Update tests to integration-style
- [x] Update README with multiplexer architecture docs

### Notes
- Static tools were causing maintenance burden (83 static vs 108 dynamic = 25 tool drift)
- Now bb-mcp has automatic parity with hive-mcp

### Files Modified
- `src/bb_mcp/core.clj` - Use `get-tools` function, call `emacs/init!` at startup
- `src/bb_mcp/tools/emacs.clj` - Simplified to dynamic-only facade
- `src/bb_mcp/tools/emacs/dynamic.clj` - Unchanged (already worked)
- Deleted: `tools/emacs/{buffer,magit,memory,kanban,swarm,projectile,org,prompt,cider,context,core}.clj`
- `test/bb_mcp/tools/dynamic_test.clj` - Consolidated integration tests
- `README.md` - Multiplexer architecture, updated tool counts

---

## Previous Sessions

### Session: 2026-01-02 (Evening)
- Created comprehensive documentation using swarm agents
- Refactored test suite to SRP structure
- All 14 tests pass with 946 assertions

### Session: 2026-01-02 (Earlier)
- Refactored emacs tools into DDD bounded context modules
- Fixed bencode to use byte-based I/O for UTF-8
- Added dynamic tool loading from hive-mcp
- Fixed magit_status directory parameter handling

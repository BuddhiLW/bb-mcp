# bb-mcp

Lightweight MCP (Model Context Protocol) server in Babashka that bridges Claude Code to Emacs via nREPL.

## Why bb-mcp?

| Metric | JVM clojure-mcp | bb-mcp |
|--------|-----------------|--------|
| Startup | ~2-3s | **~5ms** |
| Memory | ~500MB | **~50MB** |
| Per-instance overhead | Heavy | Minimal |

Multiple Claude instances share a single JVM (emacs-mcp) via nREPL, dramatically reducing resource usage.

## Architecture

```
                    bb-mcp Instances              Shared JVM
                    ┌──────────────┐
  Claude 1 ───────▶ │   bb-mcp     │──┐
                    └──────────────┘  │
                    ┌──────────────┐  │     ┌─────────────────┐
  Claude 2 ───────▶ │   bb-mcp     │──┼────▶│   emacs-mcp     │
                    └──────────────┘  │     │   (nREPL:7910)  │
                    ┌──────────────┐  │     └────────┬────────┘
  Claude 3 ───────▶ │   bb-mcp     │──┘              │
                    └──────────────┘                 ▼
                       ~50MB each              ┌─────────────┐
                                               │    Emacs    │
                                               └─────────────┘
```

**Data Flow:**
1. Claude Code connects to bb-mcp via MCP protocol (stdio)
2. bb-mcp handles native tools directly (bash, grep, file ops)
3. Emacs-related tools delegate to emacs-mcp via nREPL on port 7910
4. emacs-mcp executes elisp in Emacs via emacsclient

## Prerequisites

- [Babashka](https://babashka.org/) v1.3+
- [ripgrep](https://github.com/BurntSushi/ripgrep) (for grep tool)
- [emacs-mcp](https://github.com/your-user/emacs-mcp) running with nREPL on port 7910

## Installation

```bash
# Clone the repository
git clone https://github.com/your-user/bb-mcp.git
cd bb-mcp

# Add to Claude Code MCP config
claude mcp add bb-mcp bb -- -m bb-mcp.core
```

## Configuration

### nREPL Port Resolution

bb-mcp finds the nREPL port in this order:

1. **Explicit parameter** - `port` in tool call
2. **Environment variable** - `BB_MCP_NREPL_PORT`
3. **.nrepl-port file** - In `BB_MCP_PROJECT_DIR` or current directory
4. **Default** - Port 7910 (emacs-mcp)

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `BB_MCP_NREPL_PORT` | nREPL port to connect to | 7910 |
| `BB_MCP_PROJECT_DIR` | Directory for .nrepl-port lookup | Current dir |
| `EMACS_MCP_DIR` | emacs-mcp directory for auto-spawn | ~/dotfiles/gitthings/emacs-mcp |

## Tools

bb-mcp provides **89 tools** organized into native and delegated categories.

### Native Tools (6)

Fast tools that run directly in Babashka:

| Tool | Description |
|------|-------------|
| `bash` | Execute shell commands |
| `read_file` | Read file contents |
| `file_write` | Write files to disk |
| `glob_files` | Find files by glob pattern |
| `grep` | Search content with ripgrep |
| `clojure_eval` | Evaluate Clojure via nREPL |

### Emacs Tools (83)

Tools that delegate to emacs-mcp via nREPL:

| Domain | Count | Description |
|--------|-------|-------------|
| **Buffer** | 19 | Buffer ops, elisp eval, file navigation |
| **Magit** | 10 | Git operations via Magit |
| **Memory** | 12 | Project memory CRUD (notes, snippets, decisions) |
| **Swarm** | 11 | Claude swarm agent orchestration |
| **Kanban** | 8 | Task/kanban management |
| **CIDER** | 8 | Clojure REPL integration |
| **Projectile** | 6 | Project navigation |
| **Org** | 4 | Org-mode operations |
| **Prompt** | 4 | Prompt capture and search |
| **Context** | 1 | Full context aggregation |

### Dynamic Tool Loading

bb-mcp can dynamically load tools from emacs-mcp at startup, eliminating manual tool maintenance. If emacs-mcp is unavailable, it falls back to static tool definitions.

## Usage

### As MCP Server

```bash
# Via bb task
bb mcp

# Directly
bb -m bb-mcp.core
```

### Ensure emacs-mcp is Running

bb-mcp auto-spawns emacs-mcp if not running. To start manually:

```bash
cd ~/path/to/emacs-mcp
clojure -M:nrepl
```

## Project Structure

```
bb-mcp/
├── bb.edn                    # Babashka deps and tasks
├── src/bb_mcp/
│   ├── core.clj              # Main entry, MCP message loop
│   ├── protocol.clj          # JSON-RPC protocol handling
│   ├── nrepl_spawn.clj       # Auto-spawn emacs-mcp nREPL
│   └── tools/
│       ├── bash.clj          # Native: shell execution
│       ├── file.clj          # Native: file operations
│       ├── grep.clj          # Native: ripgrep wrapper
│       ├── nrepl.clj         # nREPL client (bencode)
│       ├── emacs.clj         # Emacs tools aggregator
│       └── emacs/
│           ├── core.clj      # Shared helpers
│           ├── buffer.clj    # Buffer domain
│           ├── magit.clj     # Git domain
│           ├── memory.clj    # Memory domain
│           ├── kanban.clj    # Kanban domain
│           ├── swarm.clj     # Swarm domain
│           ├── cider.clj     # CIDER domain
│           ├── projectile.clj# Projectile domain
│           ├── org.clj       # Org-mode domain
│           ├── prompt.clj    # Prompt domain
│           ├── context.clj   # Context domain
│           └── dynamic.clj   # Dynamic tool loading
└── test/                     # Tests
```

## Development

```bash
# Run tests
bb test

# Start REPL for development
bb nrepl
```

### Adding New Tools

1. Create a new module in `src/bb_mcp/tools/emacs/` or add to existing domain
2. Define spec and handler function
3. Add to the `tools` vector in that module
4. Re-export in `src/bb_mcp/tools/emacs.clj` if new module

### nREPL Implementation

The nREPL client (`tools/nrepl.clj`) uses byte-based bencode for proper UTF-8 handling. Key functions:

- `eval-code` - Evaluate Clojure on remote nREPL
- `bencode-to-bytes` / `bdecode-from-stream` - Binary-safe bencode

## License

MIT

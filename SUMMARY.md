# bb-mcp Summary

## What It Does

bb-mcp is a **lightweight MCP server** that connects Claude Code to Emacs. Written in Babashka, it starts in ~5ms and uses ~50MB RAM - 10x faster and lighter than JVM alternatives.

## How It Works

```
Claude Code  ─── MCP/stdio ───▶  bb-mcp  ─── nREPL:7910 ───▶  hive-mcp  ───▶  Emacs
```

- **Native tools** (bash, grep, files) run directly in Babashka
- **Emacs tools** delegate to hive-mcp via nREPL
- Multiple Claude instances share one JVM (hive-mcp)

## Current Focus Areas

1. **Tool Domains** - 89 tools organized by DDD bounded contexts:
   - Buffer, Magit, Memory, Swarm, Kanban, CIDER, Projectile, Org, Prompt, Context

2. **Dynamic Loading** - Fetch tool specs from hive-mcp at runtime

3. **Swarm Orchestration** - Spawn and manage Claude agent swarms from Emacs

## Architecture

```
bb-mcp/
├── core.clj          # MCP server entry point
├── protocol.clj      # JSON-RPC handling
├── nrepl_spawn.clj   # Auto-spawn hive-mcp
└── tools/
    ├── nrepl.clj     # Bencode nREPL client
    └── emacs/        # Domain-specific tool modules
        ├── buffer.clj   (19 tools)
        ├── magit.clj    (10 tools)
        ├── memory.clj   (12 tools)
        ├── swarm.clj    (11 tools)
        └── ...
```

## Quick Start

```bash
bb mcp                    # Start MCP server
bb test                   # Run tests
```

## Key Design Decisions

- **Babashka over JVM**: Fast startup, low memory for per-conversation instances
- **Shared nREPL**: One JVM (hive-mcp) serves many bb-mcp instances
- **Byte-based bencode**: Proper UTF-8 handling in nREPL protocol
- **DDD tool organization**: Each domain module owns its tools completely

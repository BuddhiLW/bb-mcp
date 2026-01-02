# bb-mcp SOLID + CLARITY + DDD Refactor Plan

## Current State

- `emacs.clj`: 1369 lines, monolithic
- 75 emacs wrapper tools in one file
- Helper functions inline

## Target Architecture (DDD Bounded Contexts)

```
src/bb_mcp/tools/
├── emacs/
│   ├── core.clj          # Shared: emacs-eval, wrap-emacs-call
│   ├── buffer.clj        # Buffer operations (15 tools)
│   ├── magit.clj         # Git operations (10 tools)
│   ├── memory.clj        # Memory system (12 tools)
│   ├── kanban.clj        # Kanban board (8 tools)
│   ├── swarm.clj         # Swarm orchestration (7 tools)
│   ├── projectile.clj    # Project navigation (6 tools)
│   ├── org.clj           # Org-mode (4 tools)
│   ├── prompt.clj        # Prompt capture (4 tools)
│   └── cider.clj         # CIDER REPL (8 tools)
└── emacs.clj             # Re-exports all tools, backwards compat
```

## SOLID Principles Applied

### S - Single Responsibility
Each module handles ONE domain. Example:
- `magit.clj` only handles git operations
- `memory.clj` only handles memory CRUD

### O - Open/Closed
Tool registration via data:
```clojure
;; Each module exports a `tools` vector
;; Main emacs.clj concatenates them
(def tools
  (concat buffer/tools magit/tools memory/tools ...))
```

### L - Liskov Substitution
All tool handlers follow same contract:
```clojure
(defn handler [{:keys [...] :as params}]
  {:result ... :error? bool})
```

### I - Interface Segregation
Each module exports only what's needed:
- `tools` vector for registration
- Individual handlers for direct use (optional)

### D - Dependency Inversion
Abstract nREPL communication:
```clojure
;; core.clj
(defprotocol EmacsEval
  (eval-code [this code opts]))

;; Default implementation uses nrepl
(defrecord NreplEval [port]
  EmacsEval
  (eval-code [_ code opts] ...))
```

## CLARITY Principles

1. **Consistent naming**: `handle-<domain>-<action>`
2. **Clear specs**: Each tool has `:name`, `:description`, `:schema`
3. **Documented intent**: Module docstrings explain bounded context

## Migration Strategy (Wave-based)

### Wave 1: Extract Core (No conflicts)
- Create `emacs/core.clj` with `emacs-eval`, `wrap-emacs-call`
- Update imports in `emacs.clj`

### Wave 2: Extract Domains (Parallel safe)
- Create domain modules
- Move tool definitions
- Keep `emacs.clj` as aggregator

### Wave 3: Tests
- Unit tests for each domain module
- Integration tests for tool execution

## Test Strategy (TDD)

Before refactoring, write tests that verify:
1. Tool count remains 75
2. All tool names preserved
3. Handler functions callable
4. nREPL delegation works

```clojure
;; test/bb_mcp/tools/emacs_test.clj
(ns bb-mcp.tools.emacs-test
  (:require [clojure.test :refer :all]
            [bb-mcp.tools.emacs :as emacs]))

(deftest tool-count-test
  (is (= 75 (count emacs/tools))))

(deftest all-tools-have-required-keys-test
  (doseq [tool emacs/tools]
    (is (contains? tool :spec))
    (is (contains? tool :handler))
    (is (contains? (:spec tool) :name))
    (is (contains? (:spec tool) :description))
    (is (contains? (:spec tool) :schema))))

(deftest no-duplicate-tool-names-test
  (let [names (map #(get-in % [:spec :name]) emacs/tools)]
    (is (= (count names) (count (set names))))))
```

## Commit Strategy

1. `test: Add emacs tools regression tests`
2. `refactor: Extract emacs/core.clj`
3. `refactor: Extract emacs/buffer.clj`
4. ... (one per domain)
5. `refactor: Final cleanup and docs`

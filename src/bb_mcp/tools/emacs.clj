(ns bb-mcp.tools.emacs
  "Emacs tools that delegate to emacs-mcp via shared nREPL.

   This module aggregates all domain-specific tool modules following
   DDD bounded contexts. Each module handles ONE domain:
   - buffer: Buffer operations, file handling, elisp evaluation
   - magit: Git operations via Magit
   - memory: Project memory CRUD
   - kanban: Kanban task management
   - swarm: Swarm agent orchestration
   - projectile: Project navigation
   - org: Org-mode operations
   - prompt: Prompt capture and search
   - cider: CIDER REPL integration
   - context: Full context aggregation"
  (:require [bb-mcp.tools.emacs.core :as core]
            [bb-mcp.tools.emacs.buffer :as buffer]
            [bb-mcp.tools.emacs.magit :as magit]
            [bb-mcp.tools.emacs.memory :as memory]
            [bb-mcp.tools.emacs.kanban :as kanban]
            [bb-mcp.tools.emacs.swarm :as swarm]
            [bb-mcp.tools.emacs.projectile :as projectile]
            [bb-mcp.tools.emacs.org :as org]
            [bb-mcp.tools.emacs.prompt :as prompt]
            [bb-mcp.tools.emacs.cider :as cider]
            [bb-mcp.tools.emacs.context :as context]
            [bb-mcp.tools.emacs.dynamic :as dynamic]))

;; Re-export core helpers for backwards compatibility
(def emacs-eval core/emacs-eval)
(def wrap-emacs-call core/wrap-emacs-call)

;; =============================================================================
;; Static Tools (fallback when dynamic loading fails)
;; =============================================================================

(def ^:private static-tools
  "Statically defined tools from domain modules.
   Used as fallback when emacs-mcp is not available."
  (vec (concat buffer/tools
               magit/tools
               memory/tools
               kanban/tools
               swarm/tools
               projectile/tools
               org/tools
               prompt/tools
               cider/tools
               context/tools)))

;; =============================================================================
;; Dynamic Loading
;; =============================================================================

(defn init!
  "Initialize emacs tools. Attempts dynamic loading from emacs-mcp,
   falls back to static tools on failure.

   Options:
     :port       - nREPL port (default: 7910)
     :timeout-ms - Connection timeout (default: 10000)
     :force      - Force reload even if already loaded"
  [& {:keys [port timeout-ms force] :or {port 7910 timeout-ms 10000}}]
  (when (or force (not (dynamic/tools-loaded?)))
    (dynamic/load-dynamic-tools! :port port :timeout-ms timeout-ms)))

;; =============================================================================
;; Aggregated Tools Vector
;; =============================================================================

(defn get-tools
  "Get all emacs-mcp tools. Returns dynamic tools if loaded,
   otherwise returns static tools as fallback."
  []
  (if-let [dynamic-tools (dynamic/get-tools)]
    dynamic-tools
    static-tools))

;; For backwards compatibility - static def used by MCP server registration
(def tools
  "All emacs-mcp tools. Note: For dynamic tools, call init! first,
   then use get-tools function."
  static-tools)

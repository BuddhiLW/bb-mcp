(ns bb-mcp.tools.emacs.buffer
  "Buffer-related Emacs tools.

   This module provides tools for interacting with Emacs buffers,
   files, and basic elisp evaluation."
  (:require [bb-mcp.tools.emacs.core :as core]))

;; Import shared helpers from core
(def ^:private emacs-eval core/emacs-eval)
(def ^:private wrap-emacs-call core/wrap-emacs-call)

;; =============================================================================
;; Tool: eval_elisp
;; =============================================================================

(def eval-elisp-spec
  {:name "eval_elisp"
   :description "Execute arbitrary Emacs Lisp code via emacsclient.

Requires a shared nREPL with emacs-mcp loaded (port 7910 by default).

Examples:
- eval_elisp(code: \"(buffer-name)\")
- eval_elisp(code: \"(+ 1 2 3)\")
- eval_elisp(code: \"(message \\\"Hello from bb-mcp!\\\")\")"
   :schema {:type "object"
            :properties {:code {:type "string"
                                :description "Emacs Lisp code to evaluate"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["code"]}})

(defn eval-elisp
  "Evaluate Emacs Lisp code via emacs-mcp's emacsclient."
  [{:keys [code port]}]
  (emacs-eval (wrap-emacs-call code) :port port))

;; =============================================================================
;; Tool: emacs_status
;; =============================================================================

(def emacs-status-spec
  {:name "emacs_status"
   :description "Check if Emacs server is running and get basic status."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn emacs-status
  "Check Emacs server status."
  [{:keys [port]}]
  (let [result (emacs-eval (wrap-emacs-call "(emacs-version)") :port port)]
    (if (:error? result)
      {:result (str "Emacs server not reachable: " (:result result))
       :error? true}
      {:result (str "Emacs server running: " (:result result))
       :error? false})))

;; =============================================================================
;; Tool: list_buffers
;; =============================================================================

(def list-buffers-spec
  {:name "list_buffers"
   :description "List all open buffers in Emacs."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}
                         :limit {:type "integer"
                                 :description "Max buffers to return (default: 50)"}}
            :required []}})

(defn list-buffers
  "List all open Emacs buffers."
  [{:keys [port limit]}]
  (let [limit (or limit 50)
        code (format "(mapcar #'buffer-name (seq-take (buffer-list) %d))" limit)]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: current_buffer
;; =============================================================================

(def current-buffer-spec
  {:name "current_buffer"
   :description "Get current buffer name and associated file path."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn current-buffer
  "Get current buffer info."
  [{:keys [port]}]
  (let [code "(list (buffer-name) (buffer-file-name))"]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: get_buffer_content
;; =============================================================================

(def get-buffer-content-spec
  {:name "get_buffer_content"
   :description "Get the content of a specific buffer."
   :schema {:type "object"
            :properties {:buffer_name {:type "string"
                                       :description "Name of the buffer"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["buffer_name"]}})

(defn get-buffer-content
  "Get content of a buffer."
  [{:keys [buffer_name port]}]
  (let [code (format "(with-current-buffer %s (buffer-substring-no-properties (point-min) (min (point-max) 100000)))"
                     (pr-str buffer_name))]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: find_file
;; =============================================================================

(def find-file-spec
  {:name "find_file"
   :description "Open a file in Emacs."
   :schema {:type "object"
            :properties {:file_path {:type "string"
                                     :description "Path to the file to open"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["file_path"]}})

(defn find-file
  "Open a file in Emacs."
  [{:keys [file_path port]}]
  (let [code (format "(find-file %s)" (pr-str file_path))]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: mcp_notify
;; =============================================================================

(def mcp-notify-spec
  {:name "mcp_notify"
   :description "Show a notification message in Emacs."
   :schema {:type "object"
            :properties {:message {:type "string"
                                   :description "Message to display"}
                         :type {:type "string"
                                :description "Type: info, warning, or error"
                                :enum ["info" "warning" "error"]}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["message"]}})

(defn mcp-notify
  "Show notification in Emacs."
  [{:keys [message type port]}]
  (let [msg-type (or type "info")
        code (format "(message \"[%s] %s\")" msg-type message)]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: switch_to_buffer
;; =============================================================================

(def switch-to-buffer-spec
  {:name "switch_to_buffer"
   :description "Switch to a specific buffer in Emacs."
   :schema {:type "object"
            :properties {:buffer_name {:type "string" :description "Name of buffer to switch to"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["buffer_name"]}})

(defn switch-to-buffer [{:keys [buffer_name port]}]
  (emacs-eval (wrap-emacs-call (format "(switch-to-buffer %s)" (pr-str buffer_name))) :port port))

;; =============================================================================
;; Tool: save_buffer
;; =============================================================================

(def save-buffer-spec
  {:name "save_buffer"
   :description "Save the current buffer."
   :schema {:type "object"
            :properties {:port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn save-buffer [{:keys [port]}]
  (emacs-eval (wrap-emacs-call "(save-buffer)") :port port))

;; =============================================================================
;; Tool: goto_line
;; =============================================================================

(def goto-line-spec
  {:name "goto_line"
   :description "Go to a specific line in the current buffer."
   :schema {:type "object"
            :properties {:line {:type "integer" :description "Line number to go to"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["line"]}})

(defn goto-line [{:keys [line port]}]
  (emacs-eval (wrap-emacs-call (format "(goto-line %d)" line)) :port port))

;; =============================================================================
;; Tool: insert_text
;; =============================================================================

(def insert-text-spec
  {:name "insert_text"
   :description "Insert text at point in current buffer."
   :schema {:type "object"
            :properties {:text {:type "string" :description "Text to insert"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["text"]}})

(defn insert-text [{:keys [text port]}]
  (emacs-eval (wrap-emacs-call (format "(insert %s)" (pr-str text))) :port port))

;; =============================================================================
;; Tool: recent_files
;; =============================================================================

(def recent-files-spec
  {:name "recent_files"
   :description "Get list of recently opened files."
   :schema {:type "object"
            :properties {:limit {:type "integer" :description "Max files to return (default: 20)"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn recent-files [{:keys [limit port]}]
  (let [n (or limit 20)]
    (emacs-eval (wrap-emacs-call (format "(seq-take recentf-list %d)" n)) :port port)))

;; =============================================================================
;; Tool: buffer_info
;; =============================================================================

(def buffer-info-spec
  {:name "buffer_info"
   :description "Get detailed info about a buffer (mode, modified, size, etc)."
   :schema {:type "object"
            :properties {:buffer_name {:type "string" :description "Name of buffer"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["buffer_name"]}})

(defn buffer-info [{:keys [buffer_name port]}]
  (let [code (format "(with-current-buffer %s
                        (list :name (buffer-name)
                              :file (buffer-file-name)
                              :mode major-mode
                              :modified (buffer-modified-p)
                              :size (buffer-size)
                              :point (point)))"
                     (pr-str buffer_name))]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: project_root
;; =============================================================================

(def project-root-spec
  {:name "project_root"
   :description "Get the root directory of the current project."
   :schema {:type "object"
            :properties {:port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn project-root [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools :as tools])
                  (tools/handle-project-root {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Tool: mcp_capabilities
;; =============================================================================

(def mcp-capabilities-spec
  {:name "mcp_capabilities"
   :description "Get the MCP server capabilities and available tools."
   :schema {:type "object"
            :properties {:port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn mcp-capabilities [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools :as tools])
                  (tools/handle-mcp-capabilities {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Exported tools vector
;; =============================================================================

;; =============================================================================
;; Tool: mcp_watch_buffer
;; =============================================================================

(def mcp-watch-buffer-spec
  {:name "mcp_watch_buffer"
   :description "Get recent content from a buffer for monitoring. Useful for watching *Messages*, *Warnings*, *Compile-Log*, etc. Returns the last N lines."
   :schema {:type "object"
            :properties {:buffer_name {:type "string"
                                       :description "Name of the buffer to watch (e.g., \"*Messages*\")"}
                         :lines {:type "integer"
                                 :description "Number of lines to retrieve from end (default: 50)"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["buffer_name"]}})

(defn mcp-watch-buffer
  "Get recent content from a buffer for monitoring."
  [{:keys [buffer_name lines port]}]
  (let [num-lines (or lines 50)
        code (format "(with-current-buffer %s
                       (save-excursion
                         (goto-char (point-max))
                         (forward-line (- %d))
                         (buffer-substring-no-properties (point) (point-max))))"
                     (pr-str buffer_name)
                     num-lines)]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: mcp_list_special_buffers
;; =============================================================================

(def mcp-list-special-buffers-spec
  {:name "mcp_list_special_buffers"
   :description "List all special buffers (those starting with *) useful for monitoring. Returns buffer names like *Messages*, *scratch*, *Warnings*, etc."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn mcp-list-special-buffers
  "List special buffers useful for monitoring."
  [{:keys [port]}]
  (let [code "(mapcar #'buffer-name
               (seq-filter
                 (lambda (buf)
                   (string-match-p \"^\\\\*\" (buffer-name buf)))
                 (buffer-list)))"]
    (emacs-eval (wrap-emacs-call code) :port port)))

;; =============================================================================
;; Tool: mcp_list_workflows
;; =============================================================================

(def mcp-list-workflows-spec
  {:name "mcp_list_workflows"
   :description "List available user-defined workflows. Requires emacs-mcp.el."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn mcp-list-workflows
  "List available workflows."
  [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools.buffer :as buffer])
                  (buffer/handle-mcp-list-workflows {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Tool: mcp_run_workflow
;; =============================================================================

(def mcp-run-workflow-spec
  {:name "mcp_run_workflow"
   :description "Run a user-defined workflow by name. Workflows can automate multi-step tasks. Requires emacs-mcp.el."
   :schema {:type "object"
            :properties {:name {:type "string"
                                :description "Name of the workflow to run"}
                         :args {:type "object"
                                :description "Optional arguments to pass to the workflow"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["name"]}})

(defn mcp-run-workflow
  "Run a user-defined workflow."
  [{:keys [name args port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.buffer :as buffer])
                          (buffer/handle-mcp-run-workflow {:name %s :args %s}))"
                     (pr-str name)
                     (pr-str args))]
    (emacs-eval code :port port :timeout_ms 30000)))

(def tools
  [{:spec eval-elisp-spec :handler eval-elisp}
   {:spec emacs-status-spec :handler emacs-status}
   {:spec list-buffers-spec :handler list-buffers}
   {:spec current-buffer-spec :handler current-buffer}
   {:spec get-buffer-content-spec :handler get-buffer-content}
   {:spec find-file-spec :handler find-file}
   {:spec mcp-notify-spec :handler mcp-notify}
   {:spec switch-to-buffer-spec :handler switch-to-buffer}
   {:spec save-buffer-spec :handler save-buffer}
   {:spec goto-line-spec :handler goto-line}
   {:spec insert-text-spec :handler insert-text}
   {:spec recent-files-spec :handler recent-files}
   {:spec buffer-info-spec :handler buffer-info}
   {:spec project-root-spec :handler project-root}
   {:spec mcp-capabilities-spec :handler mcp-capabilities}
   {:spec mcp-watch-buffer-spec :handler mcp-watch-buffer}
   {:spec mcp-list-special-buffers-spec :handler mcp-list-special-buffers}
   {:spec mcp-list-workflows-spec :handler mcp-list-workflows}
   {:spec mcp-run-workflow-spec :handler mcp-run-workflow}])

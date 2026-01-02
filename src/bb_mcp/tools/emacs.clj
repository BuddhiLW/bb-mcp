(ns bb-mcp.tools.emacs
  "Emacs tools that delegate to emacs-mcp via shared nREPL.

   These tools provide a lightweight way to interact with Emacs
   through bb-mcp while keeping the heavy JVM work on a shared nREPL."
  (:require [bb-mcp.tools.nrepl :as nrepl]))

;; Helper to evaluate emacs-mcp code via nREPL
(defn- emacs-eval
  "Evaluate code that uses emacs-mcp functions via shared nREPL."
  [code & {:keys [port timeout_ms]}]
  (nrepl/execute {:code code
                  :port port
                  :timeout_ms (or timeout_ms 30000)}))

(defn- wrap-emacs-call
  "Wrap an emacsclient call with require and error handling."
  [elisp-code]
  (str "(do (require '[emacs-mcp.emacsclient :as ec])"
       "    (ec/eval-elisp! " (pr-str elisp-code) "))"))

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
;; Tool: magit_status
;; =============================================================================

(def magit-status-spec
  {:name "magit_status"
   :description "Get comprehensive git status via Magit.

Returns branch, staged/unstaged/untracked files, ahead/behind counts."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn magit-status
  "Get git status via emacs-mcp's magit integration."
  [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools :as tools])
                  (tools/handle-magit-status {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Tool: mcp_get_context
;; =============================================================================

(def mcp-get-context-spec
  {:name "mcp_get_context"
   :description "Get full context from Emacs including buffer, project, git, and memory."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn mcp-get-context
  "Get full Emacs context."
  [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools :as tools])
                  (tools/handle-mcp-get-context {}))"]
    (emacs-eval code :port port :timeout_ms 30000)))

;; =============================================================================
;; Tool: mcp_kanban_status
;; =============================================================================

(def mcp-kanban-status-spec
  {:name "mcp_kanban_status"
   :description "Get kanban board status including tasks by status, progress, and backend info."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn mcp-kanban-status
  "Get kanban status via emacs-mcp."
  [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools :as tools])
                  (tools/handle-mcp-kanban-status {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Tool: mcp_memory_query
;; =============================================================================

(def mcp-memory-query-metadata-spec
  {:name "mcp_memory_query_metadata"
   :description "Query project memory metadata by type (note, snippet, convention, decision).
Returns id, type, preview, tags, created - 10x fewer tokens than full query."
   :schema {:type "object"
            :properties {:type {:type "string"
                                :description "Memory type: note, snippet, convention, decision"
                                :enum ["note" "snippet" "convention" "decision"]}
                         :limit {:type "integer"
                                 :description "Max results (default: 20)"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["type"]}})

(defn mcp-memory-query-metadata
  "Query memory metadata via emacs-mcp."
  [{:keys [type limit port]}]
  (let [code (format "(do (require '[emacs-mcp.tools :as tools])
                         (tools/handle-mcp-memory-query-metadata {:type %s :limit %d}))"
                     (pr-str type) (or limit 20))]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Tool: mcp_memory_get_full
;; =============================================================================

(def mcp-memory-get-full-spec
  {:name "mcp_memory_get_full"
   :description "Get full content of a memory entry by ID.
Use after mcp_memory_query_metadata to fetch specific entries."
   :schema {:type "object"
            :properties {:id {:type "string"
                              :description "ID of the memory entry to retrieve"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["id"]}})

(defn mcp-memory-get-full
  "Get full memory entry by ID via emacs-mcp."
  [{:keys [id port]}]
  (let [code (format "(do (require '[emacs-mcp.tools :as tools])
                         (tools/handle-mcp-memory-get-full {:id %s}))"
                     (pr-str id))]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Tool: mcp_memory_add
;; =============================================================================

(def mcp-memory-add-spec
  {:name "mcp_memory_add"
   :description "Add an entry to project memory."
   :schema {:type "object"
            :properties {:type {:type "string"
                                :description "Memory type: note, snippet, convention, decision"
                                :enum ["note" "snippet" "convention" "decision"]}
                         :content {:type "string"
                                   :description "Content to store"}
                         :tags {:type "array"
                                :items {:type "string"}
                                :description "Tags for categorization"}
                         :port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required ["type" "content"]}})

(defn mcp-memory-add
  "Add memory entry via emacs-mcp."
  [{:keys [type content tags port]}]
  (let [;; Convert tags to elisp list format
        tags-elisp (if (seq tags)
                     (str "'(" (clojure.string/join " " (map pr-str tags)) ")")
                     "nil")
        ;; Call elisp API: (emacs-mcp-api-memory-add type content &optional tags)
        elisp-code (format "(emacs-mcp-api-memory-add %s %s %s)"
                           (pr-str type) (pr-str content) tags-elisp)]
    (emacs-eval (wrap-emacs-call elisp-code) :port port :timeout_ms 15000)))

;; =============================================================================
;; Tool: swarm_status
;; =============================================================================

(def swarm-status-spec
  {:name "swarm_status"
   :description "Get swarm status including all active slaves, their states, and task counts."
   :schema {:type "object"
            :properties {:port {:type "integer"
                                :description "nREPL port (default: 7910)"}}
            :required []}})

(defn swarm-status
  "Get swarm status via emacs-mcp."
  [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools :as tools])
                  (tools/handle-swarm-status {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Additional Buffer Tools
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

(def save-buffer-spec
  {:name "save_buffer"
   :description "Save the current buffer."
   :schema {:type "object"
            :properties {:port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn save-buffer [{:keys [port]}]
  (emacs-eval (wrap-emacs-call "(save-buffer)") :port port))

(def goto-line-spec
  {:name "goto_line"
   :description "Go to a specific line in the current buffer."
   :schema {:type "object"
            :properties {:line {:type "integer" :description "Line number to go to"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["line"]}})

(defn goto-line [{:keys [line port]}]
  (emacs-eval (wrap-emacs-call (format "(goto-line %d)" line)) :port port))

(def insert-text-spec
  {:name "insert_text"
   :description "Insert text at point in current buffer."
   :schema {:type "object"
            :properties {:text {:type "string" :description "Text to insert"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["text"]}})

(defn insert-text [{:keys [text port]}]
  (emacs-eval (wrap-emacs-call (format "(insert %s)" (pr-str text))) :port port))

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
;; Additional Kanban Tools
;; =============================================================================

(def mcp-kanban-list-tasks-spec
  {:name "mcp_kanban_list_tasks"
   :description "List kanban tasks, optionally filtered by status."
   :schema {:type "object"
            :properties {:status {:type "string" :description "Filter by status: todo, inprogress, done"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn mcp-kanban-list-tasks [{:keys [status port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.kanban :as k])
                         (k/handle-mcp-kanban-list-tasks {:status %s}))"
                     (if status (pr-str status) "nil"))]
    (emacs-eval code :port port :timeout_ms 15000)))

(def mcp-kanban-create-task-spec
  {:name "mcp_kanban_create_task"
   :description "Create a new kanban task."
   :schema {:type "object"
            :properties {:title {:type "string" :description "Task title"}
                         :description {:type "string" :description "Task description"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["title"]}})

(defn mcp-kanban-create-task [{:keys [title description port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.kanban :as k])
                         (k/handle-mcp-kanban-create-task {:title %s :description %s}))"
                     (pr-str title) (if description (pr-str description) "nil"))]
    (emacs-eval code :port port :timeout_ms 15000)))

(def mcp-kanban-move-task-spec
  {:name "mcp_kanban_move_task"
   :description "Move a kanban task to a new status."
   :schema {:type "object"
            :properties {:task_id {:type "string" :description "Task ID"}
                         :new_status {:type "string" :description "New status: todo, inprogress, done"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["task_id" "new_status"]}})

(defn mcp-kanban-move-task [{:keys [task_id new_status port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.kanban :as k])
                         (k/handle-mcp-kanban-move-task {:task_id %s :new_status %s}))"
                     (pr-str task_id) (pr-str new_status))]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Additional Magit Tools
;; =============================================================================

(def magit-branches-spec
  {:name "magit_branches"
   :description "List git branches via Magit."
   :schema {:type "object"
            :properties {:directory {:type "string" :description "Repository directory"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn magit-branches [{:keys [directory port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.magit :as m])
                         (m/handle-magit-branches {:directory %s}))"
                     (if directory (pr-str directory) "nil"))]
    (emacs-eval code :port port :timeout_ms 15000)))

(def magit-log-spec
  {:name "magit_log"
   :description "Get git log via Magit."
   :schema {:type "object"
            :properties {:count {:type "integer" :description "Number of commits (default: 10)"}
                         :directory {:type "string" :description "Repository directory"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn magit-log [{:keys [count directory port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.magit :as m])
                         (m/handle-magit-log {:count %d :directory %s}))"
                     (or count 10) (if directory (pr-str directory) "nil"))]
    (emacs-eval code :port port :timeout_ms 15000)))

(def magit-diff-spec
  {:name "magit_diff"
   :description "Get git diff via Magit."
   :schema {:type "object"
            :properties {:target {:type "string" :description "Diff target (e.g., HEAD~1, branch name)"}
                         :directory {:type "string" :description "Repository directory"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn magit-diff [{:keys [target directory port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.magit :as m])
                         (m/handle-magit-diff {:target %s :directory %s}))"
                     (if target (pr-str target) "nil")
                     (if directory (pr-str directory) "nil"))]
    (emacs-eval code :port port :timeout_ms 15000)))

(def magit-stage-spec
  {:name "magit_stage"
   :description "Stage files for commit via Magit."
   :schema {:type "object"
            :properties {:files {:type "array" :items {:type "string"} :description "Files to stage (empty = all)"}
                         :directory {:type "string" :description "Repository directory"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn magit-stage [{:keys [files directory port]}]
  (let [files-str (if (seq files)
                    (str "'(" (clojure.string/join " " (map pr-str files)) ")")
                    "nil")
        code (format "(do (require '[emacs-mcp.tools.magit :as m])
                         (m/handle-magit-stage {:files %s :directory %s}))"
                     files-str (if directory (pr-str directory) "nil"))]
    (emacs-eval code :port port :timeout_ms 15000)))

(def magit-commit-spec
  {:name "magit_commit"
   :description "Create a git commit via Magit."
   :schema {:type "object"
            :properties {:message {:type "string" :description "Commit message"}
                         :all {:type "boolean" :description "Stage all changes before commit"}
                         :directory {:type "string" :description "Repository directory"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["message"]}})

(defn magit-commit [{:keys [message all directory port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.magit :as m])
                         (m/handle-magit-commit {:message %s :all %s :directory %s}))"
                     (pr-str message) (if all "t" "nil")
                     (if directory (pr-str directory) "nil"))]
    (emacs-eval code :port port :timeout_ms 30000)))

(def magit-push-spec
  {:name "magit_push"
   :description "Push commits via Magit."
   :schema {:type "object"
            :properties {:set_upstream {:type "boolean" :description "Set upstream tracking"}
                         :directory {:type "string" :description "Repository directory"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn magit-push [{:keys [set_upstream directory port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.magit :as m])
                         (m/handle-magit-push {:set_upstream %s :directory %s}))"
                     (if set_upstream "t" "nil")
                     (if directory (pr-str directory) "nil"))]
    (emacs-eval code :port port :timeout_ms 60000)))

(def magit-pull-spec
  {:name "magit_pull"
   :description "Pull changes via Magit."
   :schema {:type "object"
            :properties {:directory {:type "string" :description "Repository directory"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn magit-pull [{:keys [directory port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.magit :as m])
                         (m/handle-magit-pull {:directory %s}))"
                     (if directory (pr-str directory) "nil"))]
    (emacs-eval code :port port :timeout_ms 60000)))

;; =============================================================================
;; Additional Swarm Tools
;; =============================================================================

(def swarm-spawn-spec
  {:name "swarm_spawn"
   :description "Spawn a new Claude swarm slave."
   :schema {:type "object"
            :properties {:name {:type "string" :description "Slave name"}
                         :presets {:type "array" :items {:type "string"} :description "Preset names to load"}
                         :cwd {:type "string" :description "Working directory"}
                         :role {:type "string" :description "Agent role description"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn swarm-spawn [{:keys [name presets cwd role port]}]
  (let [presets-str (if (seq presets)
                      (str "'(" (clojure.string/join " " (map pr-str presets)) ")")
                      "nil")
        code (format "(do (require '[emacs-mcp.tools.swarm :as s])
                         (s/handle-swarm-spawn {:name %s :presets %s :cwd %s :role %s}))"
                     (if name (pr-str name) "nil")
                     presets-str
                     (if cwd (pr-str cwd) "nil")
                     (if role (pr-str role) "nil"))]
    (emacs-eval code :port port :timeout_ms 30000)))

(def swarm-dispatch-spec
  {:name "swarm_dispatch"
   :description "Dispatch a prompt to a swarm slave."
   :schema {:type "object"
            :properties {:slave_id {:type "string" :description "Slave ID to dispatch to"}
                         :prompt {:type "string" :description "Prompt to send"}
                         :timeout_ms {:type "integer" :description "Timeout in ms"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["slave_id" "prompt"]}})

(defn swarm-dispatch [{:keys [slave_id prompt timeout_ms port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.swarm :as s])
                         (s/handle-swarm-dispatch {:slave_id %s :prompt %s :timeout_ms %s}))"
                     (pr-str slave_id) (pr-str prompt)
                     (if timeout_ms (str timeout_ms) "nil"))]
    (emacs-eval code :port port :timeout_ms (or timeout_ms 60000))))

(def swarm-collect-spec
  {:name "swarm_collect"
   :description "Collect results from a dispatched task."
   :schema {:type "object"
            :properties {:task_id {:type "string" :description "Task ID to collect"}
                         :timeout_ms {:type "integer" :description "Timeout in ms"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["task_id"]}})

(defn swarm-collect [{:keys [task_id timeout_ms port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.swarm :as s])
                         (s/handle-swarm-collect {:task_id %s :timeout_ms %s}))"
                     (pr-str task_id)
                     (if timeout_ms (str timeout_ms) "nil"))]
    (emacs-eval code :port port :timeout_ms (or timeout_ms 60000))))

(def swarm-list-presets-spec
  {:name "swarm_list_presets"
   :description "List available swarm presets."
   :schema {:type "object"
            :properties {:port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn swarm-list-presets [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools.swarm :as s])
                  (s/handle-swarm-list-presets {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

(def swarm-kill-spec
  {:name "swarm_kill"
   :description "Kill a swarm slave."
   :schema {:type "object"
            :properties {:slave_id {:type "string" :description "Slave ID to kill"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["slave_id"]}})

(defn swarm-kill [{:keys [slave_id port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.swarm :as s])
                         (s/handle-swarm-kill {:slave_id %s}))"
                     (pr-str slave_id))]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Additional Memory Tools
;; =============================================================================

(def mcp-memory-search-semantic-spec
  {:name "mcp_memory_search_semantic"
   :description "Semantic search across project memory using embeddings."
   :schema {:type "object"
            :properties {:query {:type "string" :description "Search query"}
                         :limit {:type "integer" :description "Max results (default: 10)"}
                         :type {:type "string" :description "Filter by type"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["query"]}})

(defn mcp-memory-search-semantic [{:keys [query limit type port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.memory :as m])
                         (m/handle-mcp-memory-search-semantic {:query %s :limit %s :type %s}))"
                     (pr-str query)
                     (if limit (str limit) "nil")
                     (if type (pr-str type) "nil"))]
    (emacs-eval code :port port :timeout_ms 30000)))

(def mcp-memory-set-duration-spec
  {:name "mcp_memory_set_duration"
   :description "Set duration/expiry for a memory entry."
   :schema {:type "object"
            :properties {:id {:type "string" :description "Memory entry ID"}
                         :duration {:type "string" :description "Duration: ephemeral, short-term, medium-term, long-term, permanent"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["id" "duration"]}})

(defn mcp-memory-set-duration [{:keys [id duration port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.memory :as m])
                         (m/handle-mcp-memory-set-duration {:id %s :duration %s}))"
                     (pr-str id) (pr-str duration))]
    (emacs-eval code :port port :timeout_ms 15000)))

(def mcp-memory-promote-spec
  {:name "mcp_memory_promote"
   :description "Promote a memory entry to longer duration."
   :schema {:type "object"
            :properties {:id {:type "string" :description "Memory entry ID"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["id"]}})

(defn mcp-memory-promote [{:keys [id port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.memory :as m])
                         (m/handle-mcp-memory-promote {:id %s}))"
                     (pr-str id))]
    (emacs-eval code :port port :timeout_ms 15000)))

;; =============================================================================
;; Projectile Tools
;; =============================================================================

(def projectile-info-spec
  {:name "projectile_info"
   :description "Get current project info via Projectile."
   :schema {:type "object"
            :properties {:port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn projectile-info [{:keys [port]}]
  (let [code "(do (require '[emacs-mcp.tools.projectile :as p])
                  (p/handle-projectile-info {}))"]
    (emacs-eval code :port port :timeout_ms 15000)))

(def projectile-files-spec
  {:name "projectile_files"
   :description "List project files, optionally filtered by pattern."
   :schema {:type "object"
            :properties {:pattern {:type "string" :description "Glob pattern to filter files"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required []}})

(defn projectile-files [{:keys [pattern port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.projectile :as p])
                         (p/handle-projectile-files {:pattern %s}))"
                     (if pattern (pr-str pattern) "nil"))]
    (emacs-eval code :port port :timeout_ms 30000)))

(def projectile-search-spec
  {:name "projectile_search"
   :description "Search for pattern in project files."
   :schema {:type "object"
            :properties {:pattern {:type "string" :description "Search pattern"}
                         :port {:type "integer" :description "nREPL port (default: 7910)"}}
            :required ["pattern"]}})

(defn projectile-search [{:keys [pattern port]}]
  (let [code (format "(do (require '[emacs-mcp.tools.projectile :as p])
                         (p/handle-projectile-search {:pattern %s}))"
                     (pr-str pattern))]
    (emacs-eval code :port port :timeout_ms 30000)))

;; =============================================================================
;; All tool specs and handlers
;; =============================================================================

(def tools
  [;; Basic Emacs tools
   {:spec eval-elisp-spec :handler eval-elisp}
   {:spec emacs-status-spec :handler emacs-status}
   {:spec list-buffers-spec :handler list-buffers}
   {:spec current-buffer-spec :handler current-buffer}
   {:spec get-buffer-content-spec :handler get-buffer-content}
   {:spec find-file-spec :handler find-file}
   {:spec mcp-notify-spec :handler mcp-notify}
   ;; Additional Buffer tools
   {:spec switch-to-buffer-spec :handler switch-to-buffer}
   {:spec save-buffer-spec :handler save-buffer}
   {:spec goto-line-spec :handler goto-line}
   {:spec insert-text-spec :handler insert-text}
   {:spec recent-files-spec :handler recent-files}
   {:spec buffer-info-spec :handler buffer-info}
   ;; Magit integration
   {:spec magit-status-spec :handler magit-status}
   {:spec magit-branches-spec :handler magit-branches}
   {:spec magit-log-spec :handler magit-log}
   {:spec magit-diff-spec :handler magit-diff}
   {:spec magit-stage-spec :handler magit-stage}
   {:spec magit-commit-spec :handler magit-commit}
   {:spec magit-push-spec :handler magit-push}
   {:spec magit-pull-spec :handler magit-pull}
   ;; Context & Memory
   {:spec mcp-get-context-spec :handler mcp-get-context}
   {:spec mcp-memory-query-metadata-spec :handler mcp-memory-query-metadata}
   {:spec mcp-memory-get-full-spec :handler mcp-memory-get-full}
   {:spec mcp-memory-add-spec :handler mcp-memory-add}
   {:spec mcp-memory-search-semantic-spec :handler mcp-memory-search-semantic}
   {:spec mcp-memory-set-duration-spec :handler mcp-memory-set-duration}
   {:spec mcp-memory-promote-spec :handler mcp-memory-promote}
   ;; Kanban
   {:spec mcp-kanban-status-spec :handler mcp-kanban-status}
   {:spec mcp-kanban-list-tasks-spec :handler mcp-kanban-list-tasks}
   {:spec mcp-kanban-create-task-spec :handler mcp-kanban-create-task}
   {:spec mcp-kanban-move-task-spec :handler mcp-kanban-move-task}
   ;; Swarm
   {:spec swarm-status-spec :handler swarm-status}
   {:spec swarm-spawn-spec :handler swarm-spawn}
   {:spec swarm-dispatch-spec :handler swarm-dispatch}
   {:spec swarm-collect-spec :handler swarm-collect}
   {:spec swarm-list-presets-spec :handler swarm-list-presets}
   {:spec swarm-kill-spec :handler swarm-kill}
   ;; Projectile
   {:spec projectile-info-spec :handler projectile-info}
   {:spec projectile-files-spec :handler projectile-files}
   {:spec projectile-search-spec :handler projectile-search}])

(ns bb-mcp.nrepl-spawn
  "Auto-spawn emacs-mcp nREPL if not running.

   This enables bb-mcp to work seamlessly without manual nREPL setup.
   If the nREPL is not running, it spawns emacs-mcp's nREPL server
   in the background and waits for it to be ready."
  (:require [babashka.process :as p]
            [clojure.java.io :as io]))

(def default-port 7910)

(def emacs-mcp-dir
  "Directory containing emacs-mcp project with deps.edn."
  (or (System/getenv "EMACS_MCP_DIR")
      (str (System/getenv "HOME") "/dotfiles/gitthings/emacs-mcp")))

(defn nrepl-alive?
  "Check if nREPL is listening on port by attempting a socket connection."
  [port]
  (try
    (with-open [sock (java.net.Socket. "localhost" port)]
      true)
    (catch Exception _
      false)))

(defn spawn-nrepl!
  "Spawn emacs-mcp nREPL server in background.
   Logs to /tmp/emacs-mcp-nrepl.log"
  []
  (let [log-file (io/file "/tmp/emacs-mcp-nrepl.log")]
    ;; Ensure log file exists
    (spit log-file (str "=== Starting nREPL at " (java.time.Instant/now) " ===\n") :append true)
    (p/process {:dir emacs-mcp-dir
                :out :append
                :out-file log-file
                :err :append
                :err-file log-file}
               "clojure" "-M:nrepl")))

(defn ensure-nrepl!
  "Ensure nREPL is running on port. Spawns if needed.

   Returns true if nREPL is available (was already running or successfully spawned),
   false if spawn failed after timeout."
  ([] (ensure-nrepl! default-port))
  ([port]
   (if (nrepl-alive? port)
     true
     (do
       (binding [*out* *err*]
         (println "bb-mcp: nREPL not found on port" port "- spawning emacs-mcp..."))
       (spawn-nrepl!)
       ;; Wait for port to become available (max 30 seconds)
       (loop [attempts 30]
         (cond
           (nrepl-alive? port)
           (do
             (binding [*out* *err*]
               (println "bb-mcp: nREPL ready on port" port))
             true)

           (pos? attempts)
           (do
             (Thread/sleep 1000)
             (recur (dec attempts)))

           :else
           (do
             (binding [*out* *err*]
               (println "bb-mcp: WARNING - nREPL failed to start after 30s"))
             false)))))))

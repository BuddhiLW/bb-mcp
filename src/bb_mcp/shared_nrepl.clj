(ns bb-mcp.shared-nrepl
  "Shared nREPL connection manager.
   Multiple bb-mcp instances share ONE nREPL per project."
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

(defn find-nrepl-port
  "Find nREPL port from .nrepl-port file."
  [project-dir]
  (let [port-file (str project-dir "/.nrepl-port")]
    (when (fs/exists? port-file)
      (parse-long (str/trim (slurp port-file))))))

(defn nrepl-alive?
  "Check if nREPL is responding on given port."
  [port]
  (try
    (let [socket (java.net.Socket. "localhost" port)]
      (.close socket)
      true)
    (catch Exception _ false)))

(defn start-shared-nrepl!
  "Start a shared nREPL for the project if not already running.
   Returns the port number."
  [project-dir & {:keys [start-cmd port]}]
  (let [existing-port (find-nrepl-port project-dir)]
    (cond
      ;; Already running
      (and existing-port (nrepl-alive? existing-port))
      {:status :existing :port existing-port}

      ;; Start new nREPL
      start-cmd
      (do
        (p/process {:dir project-dir
                    :out :inherit
                    :err :inherit}
                   start-cmd)
        ;; Wait for .nrepl-port file
        (loop [tries 30]
          (Thread/sleep 1000)
          (if-let [new-port (find-nrepl-port project-dir)]
            {:status :started :port new-port}
            (if (pos? tries)
              (recur (dec tries))
              {:status :failed :error "nREPL did not start in time"}))))

      :else
      {:status :not-running :error "No nREPL found and no start-cmd provided"})))

(defn get-shared-port
  "Get the shared nREPL port for a project.
   Reads from .nrepl-port or environment."
  [project-dir]
  (or (some-> (System/getenv "BB_MCP_NREPL_PORT") parse-long)
      (find-nrepl-port project-dir)))

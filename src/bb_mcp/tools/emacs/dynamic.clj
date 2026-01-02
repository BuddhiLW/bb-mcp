(ns bb-mcp.tools.emacs.dynamic
  "Dynamic tool loading from emacs-mcp.

   This module fetches tool specs from emacs-mcp at startup and creates
   forwarding handlers, eliminating the need for manual tool maintenance.

   Flow:
   1. Query emacs-mcp for all tool specs via nREPL
   2. Transform specs from emacs-mcp format to bb-mcp format
   3. Create forwarding handlers that delegate to emacs-mcp
   4. Cache tools in atom for session lifetime"
  (:require [bb-mcp.tools.nrepl :as nrepl]
            [cheshire.core :as json]
            [clojure.edn :as edn]))

(defonce ^:private tool-cache (atom nil))

(defn- fetch-emacs-tools-raw
  "Query emacs-mcp for all tool specs via nREPL.
   Returns raw tool data or nil on failure.

   emacs-mcp tools are flat: {:name, :description, :inputSchema, :handler}
   We extract just the spec fields (not handler)."
  [{:keys [port timeout-ms] :or {port 7910 timeout-ms 10000}}]
  (let [code "(do
                (require '[emacs-mcp.tools :as tools])
                (pr-str
                  (mapv (fn [t]
                          {:name (:name t)
                           :description (:description t)
                           :schema (:inputSchema t)})
                        tools/tools)))"]
    (try
      (let [result (nrepl/eval-code {:port port
                                     :code code
                                     :timeout-ms timeout-ms})]
        (when-not (:error? result)
          ;; Result is double-quoted: nREPL returns pr-str of our pr-str
          ;; First read unwraps the outer quotes, second reads the EDN
          (-> (:result result)
              edn/read-string   ; unwrap outer quotes
              edn/read-string))) ; parse EDN vector
      (catch Exception e
        (println "[dynamic] Failed to fetch tools:" (ex-message e))
        nil))))

(defn- make-forwarding-handler
  "Create a handler that forwards calls to emacs-mcp via nREPL."
  [tool-name]
  (fn [args]
    (let [port (or (:port args) 7910)
          ;; Remove port from args passed to emacs-mcp
          emacs-args (dissoc args :port)
          code (str "(do
                       (require '[emacs-mcp.tools :as tools])
                       (let [tool (first (filter #(= (:name %) \"" tool-name "\")
                                                  tools/tools))
                             handler (:handler tool)]
                         (handler " (pr-str emacs-args) ")))")]
      (nrepl/eval-code {:port port
                        :code code
                        :timeout-ms (or (:timeout_ms args) 30000)}))))

(defn- transform-tool
  "Transform emacs-mcp tool spec to bb-mcp format.
   emacs-mcp: {:name, :description, :schema}
   bb-mcp: {:spec {:name, :description, :schema}, :handler fn}"
  [{:keys [name description schema]}]
  {:spec {:name name
          :description description
          :schema (or schema {:type "object" :properties {} :required []})}
   :handler (make-forwarding-handler name)})

(defn load-dynamic-tools!
  "Fetch tools from emacs-mcp and cache them.
   Returns true on success, false on failure."
  [& {:keys [port timeout-ms] :or {port 7910 timeout-ms 10000}}]
  (println "[dynamic] Loading tools from emacs-mcp on port" port)
  (if-let [raw-tools (fetch-emacs-tools-raw {:port port :timeout-ms timeout-ms})]
    (let [tools (mapv transform-tool raw-tools)]
      (reset! tool-cache tools)
      (println "[dynamic] Loaded" (count tools) "tools from emacs-mcp")
      true)
    (do
      (println "[dynamic] Failed to load tools, using static fallback")
      false)))

(defn get-tools
  "Get cached dynamic tools. Returns nil if not loaded."
  []
  @tool-cache)

(defn tools-loaded?
  "Check if dynamic tools have been loaded."
  []
  (some? @tool-cache))

(defn clear-cache!
  "Clear the tool cache (for testing/reload)."
  []
  (reset! tool-cache nil))

;; Empty tools vector - actual tools are dynamically loaded
(def tools [])

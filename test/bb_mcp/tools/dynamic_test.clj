(ns bb-mcp.tools.dynamic-test
  "Tests for dynamic tool loading from emacs-mcp."
  (:require [clojure.test :refer :all]
            [bb-mcp.tools.emacs :as emacs]
            [bb-mcp.tools.emacs.dynamic :as dynamic]))

;; =============================================================================
;; Unit Tests - Transform Functions (no emacs-mcp required)
;; =============================================================================

(deftest transform-tool-test
  (testing "Transform emacs-mcp spec to bb-mcp format"
    (let [emacs-spec {:name "test_tool"
                      :description "A test tool"
                      :schema {:type "object"
                               :properties {:foo {:type "string"}}
                               :required ["foo"]}}
          result (#'dynamic/transform-tool emacs-spec)]
      (is (map? result) "Result should be a map")
      (is (contains? result :spec) "Result should have :spec")
      (is (contains? result :handler) "Result should have :handler")
      (is (fn? (:handler result)) "Handler should be a function")
      (is (= "test_tool" (get-in result [:spec :name])))
      (is (= "A test tool" (get-in result [:spec :description])))
      (is (= {:type "object"
              :properties {:foo {:type "string"}}
              :required ["foo"]}
             (get-in result [:spec :schema]))))))

(deftest transform-tool-missing-schema-test
  (testing "Transform handles missing schema"
    (let [emacs-spec {:name "minimal_tool"
                      :description "Minimal tool"
                      :schema nil}
          result (#'dynamic/transform-tool emacs-spec)]
      (is (= {:type "object" :properties {} :required []}
             (get-in result [:spec :schema]))
          "Missing schema should get default empty schema"))))

;; =============================================================================
;; Unit Tests - Cache Functions (no emacs-mcp required)
;; =============================================================================

(deftest cache-lifecycle-test
  (testing "Cache lifecycle"
    ;; Start clean
    (dynamic/clear-cache!)
    (is (nil? (dynamic/get-tools)) "Cache should be nil initially")
    (is (false? (dynamic/tools-loaded?)) "tools-loaded? should be false")

    ;; After clearing, still nil
    (dynamic/clear-cache!)
    (is (nil? (dynamic/get-tools)) "Cache should still be nil after clear")))

;; =============================================================================
;; Unit Tests - Forwarding Handler (no emacs-mcp required)
;; =============================================================================

(deftest make-forwarding-handler-test
  (testing "Forwarding handler is created correctly"
    (let [handler (#'dynamic/make-forwarding-handler "test_tool")]
      (is (fn? handler) "Should return a function"))))

;; =============================================================================
;; Integration Tests - Dynamic Loading (requires emacs-mcp on port 7910)
;; =============================================================================

(defn emacs-mcp-available?
  "Check if emacs-mcp nREPL is available."
  []
  (try
    (dynamic/clear-cache!)
    (emacs/init!)
    (dynamic/tools-loaded?)
    (catch Exception _ false)))

(deftest dynamic-loading-integration-test
  (when (emacs-mcp-available?)
    (testing "Dynamic tool loading from emacs-mcp"
      (let [tools (emacs/get-tools)]
        (is (> (count tools) 50)
            "Should load many tools from emacs-mcp")

        (testing "All tools have required structure"
          (doseq [tool tools]
            (is (map? tool) "Tool must be a map")
            (is (contains? tool :spec) "Missing :spec")
            (is (contains? tool :handler) "Missing :handler")
            (is (fn? (:handler tool)) "Handler must be function")))

        (testing "All specs have required keys"
          (doseq [{:keys [spec]} tools]
            (is (string? (:name spec)) "Missing :name")
            (is (string? (:description spec)) "Missing :description")
            (is (map? (:schema spec)) "Missing :schema")))

        (testing "No duplicate tool names"
          (let [names (map #(get-in % [:spec :name]) tools)
                freq (frequencies names)
                dups (filter #(> (val %) 1) freq)]
            (is (empty? dups)
                (str "Duplicate tools: " (keys dups)))))

        (testing "Critical tools present"
          (let [names (set (map #(get-in % [:spec :name]) tools))
                critical ["emacs_status" "eval_elisp" "magit_status"
                          "mcp_get_context" "mcp_kanban_status"
                          "swarm_status"]]
            (doseq [tool critical]
              (is (contains? names tool)
                  (str "Critical tool missing: " tool)))))))))

(deftest empty-tools-when-not-initialized-test
  (testing "get-tools returns empty when not initialized"
    (dynamic/clear-cache!)
    (is (= [] (emacs/get-tools))
        "Should return empty vector when cache is nil")))

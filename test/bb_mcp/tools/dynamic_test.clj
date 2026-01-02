(ns bb-mcp.tools.dynamic-test
  "Tests for dynamic tool loading from emacs-mcp."
  (:require [clojure.test :refer :all]
            [bb-mcp.tools.emacs.dynamic :as dynamic]))

;; =============================================================================
;; Unit Tests - Transform Functions
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
;; Unit Tests - Cache Functions
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
;; Unit Tests - Forwarding Handler
;; =============================================================================

(deftest make-forwarding-handler-test
  (testing "Forwarding handler is created correctly"
    (let [handler (#'dynamic/make-forwarding-handler "test_tool")]
      (is (fn? handler) "Should return a function"))))

;; =============================================================================
;; Integration Tests - Tools Vector
;; =============================================================================

(deftest static-tools-vector-test
  (testing "Static tools vector is empty"
    (is (= [] dynamic/tools)
        "dynamic/tools should be empty vector (actual tools are cached)")))

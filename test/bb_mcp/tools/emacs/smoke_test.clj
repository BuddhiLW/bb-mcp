(ns bb-mcp.tools.emacs.smoke-test
  "Smoke tests for critical tools existence checks."
  (:require [clojure.test :refer :all]
            [bb-mcp.tools.emacs :as emacs]))

(deftest critical-tools-exist-test
  (testing "Critical tools for basic operation exist"
    (let [tool-names (set (map #(get-in % [:spec :name]) emacs/tools))
          critical-tools ["emacs_status" "eval_elisp" "magit_status"
                          "mcp_get_context" "mcp_kanban_status"
                          "mcp_memory_query_metadata" "swarm_status"]]
      (doseq [tool critical-tools]
        (is (contains? tool-names tool)
            (str "Critical tool missing: " tool))))))

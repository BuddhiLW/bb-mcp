(ns bb-mcp.test-runner
  "Test runner for bb-mcp."
  (:require [clojure.test :as test]
            ;; SRP-structured emacs tests
            [bb-mcp.tools.emacs.regression-test]
            [bb-mcp.tools.emacs.structure-test]
            [bb-mcp.tools.emacs.categories-test]
            [bb-mcp.tools.emacs.smoke-test]
            ;; Dynamic tool loading tests
            [bb-mcp.tools.dynamic-test]))

(defn -main [& _args]
  (let [result (test/run-tests
                'bb-mcp.tools.emacs.regression-test
                'bb-mcp.tools.emacs.structure-test
                'bb-mcp.tools.emacs.categories-test
                'bb-mcp.tools.emacs.smoke-test
                'bb-mcp.tools.dynamic-test)]
    (System/exit (if (and (zero? (:fail result))
                          (zero? (:error result)))
                   0
                   1))))

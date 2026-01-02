(ns bb-mcp.test-runner
  "Test runner for bb-mcp."
  (:require [clojure.test :as test]
            [bb-mcp.tools.emacs-test]))

(defn -main [& _args]
  (let [result (test/run-tests 'bb-mcp.tools.emacs-test)]
    (System/exit (if (and (zero? (:fail result))
                          (zero? (:error result)))
                   0
                   1))))

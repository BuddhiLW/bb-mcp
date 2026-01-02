(ns bb-mcp.tools.emacs.regression-test
  "Regression tests for tool count stability and backwards compatibility."
  (:require [clojure.test :refer :all]
            [bb-mcp.tools.emacs :as emacs]))

(deftest tool-count-regression-test
  (testing "Total tool count must remain stable"
    (is (= 83 (count emacs/tools))
        "Tool count changed - this breaks backwards compatibility")))

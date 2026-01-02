(ns bb-mcp.tools.emacs.structure-test
  "Tool structure validation tests for :spec, :handler, :schema keys."
  (:require [clojure.test :refer :all]
            [bb-mcp.tools.emacs :as emacs]))

(deftest all-tools-have-required-structure-test
  (testing "All tools must have :spec and :handler keys"
    (doseq [tool emacs/tools]
      (is (map? tool) "Tool must be a map")
      (is (contains? tool :spec) (str "Missing :spec in tool"))
      (is (contains? tool :handler) (str "Missing :handler in tool")))))

(deftest all-specs-have-required-keys-test
  (testing "All specs must have :name, :description, :schema"
    (doseq [{:keys [spec]} emacs/tools]
      (is (string? (:name spec))
          (str "Missing or invalid :name in spec"))
      (is (string? (:description spec))
          (str "Missing or invalid :description for " (:name spec)))
      (is (map? (:schema spec))
          (str "Missing or invalid :schema for " (:name spec))))))

(deftest no-duplicate-tool-names-test
  (testing "Tool names must be unique"
    (let [names (map #(get-in % [:spec :name]) emacs/tools)
          freq (frequencies names)
          duplicates (filter #(> (val %) 1) freq)]
      (is (empty? duplicates)
          (str "Duplicate tool names found: " (keys duplicates))))))

(deftest all-handlers-are-functions-test
  (testing "All handlers must be callable functions"
    (doseq [{:keys [spec handler]} emacs/tools]
      (is (fn? handler)
          (str "Handler for " (:name spec) " is not a function")))))

(deftest all-schemas-are-valid-json-schema-test
  (testing "All schemas must be valid JSON Schema structure"
    (doseq [{:keys [spec]} emacs/tools]
      (let [schema (:schema spec)]
        (is (= "object" (:type schema))
            (str "Schema type must be 'object' for " (:name spec)))
        (is (map? (:properties schema))
            (str "Schema must have :properties for " (:name spec)))
        (is (or (nil? (:required schema))
                (vector? (:required schema)))
            (str "Schema :required must be nil or vector for " (:name spec)))))))

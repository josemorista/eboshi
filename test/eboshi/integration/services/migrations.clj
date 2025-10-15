(ns eboshi.integration.services.migrations
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [eboshi.services.migrations :as services.migrations]
            [eboshi.logic.migrations :as logic.migrations]
            [eboshi.generators.migrations :as gen.migrations]
            [eboshi.logic.fs :as fs]
            [clojure.edn :as edn]
            [clojure.test.check.generators :as gen]
            [clojure.string :as cstr]))

(def ^:dynamic *config* nil)

(use-fixtures :once st/validate-schemas (fn [f]
                                          (binding [*config* (logic.migrations/make-config (->> (random-uuid)
                                                                                                str
                                                                                                (fs/join-path (System/getProperty "java.io.tmpdir"))))]
                                            (f)
                                            (fs/rm! (:migrations-dir *config*)))))

(deftest create!-migration-test
  (testing "It should create migration file"
    (let [dummy-migration-name (gen/generate gen.migrations/migration-name-gen)]
      (services.migrations/create! *config* dummy-migration-name)
      (let [migrations-dir (:migrations-dir *config*)
            [migration] (fs/ls migrations-dir :short)
            expected-migration-name (cstr/replace migration #"\.edn" "")]
        (is (re-matches (re-pattern (str "^\\d+-" dummy-migration-name "\\.edn")) migration))
        (is (= {:name expected-migration-name :up [] :down []}
               (edn/read-string (slurp (fs/join-path migrations-dir migration)))))))))




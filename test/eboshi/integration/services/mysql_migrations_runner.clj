(ns eboshi.integration.services.mysql-migrations-runner
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [eboshi.services.migrations :as services.migrations]
            [eboshi.logic.migrations :as logic.migrations]
            [eboshi.logic.fs :as fs]
            [clojure.edn :as edn]))

(def ^:dynamic *config* nil)

(use-fixtures :once st/validate-schemas (fn [f]
                                          (binding [*config* (logic.migrations/make-config (->> (random-uuid)
                                                                                                str
                                                                                                (fs/join-path (System/getProperty "java.io.tmpdir"))))]
                                            (f)

                                            (fs/rm! (:migrations-dir *config*)))))

(deftest up-migration-test
  (testing "It should return nil if there are no migrations pending"
    ()))




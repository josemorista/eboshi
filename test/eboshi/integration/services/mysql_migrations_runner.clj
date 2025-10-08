(ns eboshi.integration.services.mysql-migrations-runner
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [eboshi.services.migrations :as services.migrations]
            [eboshi.logic.migrations :as logic.migrations]
            [eboshi.logic.fs :as fs]
            [eboshi.infra.mysql-migration-runner :as runners.mysql]
            [clojure.edn :as edn]))

(def db-spec {:dbtype "mysql"
              :dbname "items"
              :user "root"
              :password "docker"})

(def ^:dynamic *config* nil)
(def ^:dynamic *runner* nil)

(use-fixtures :once st/validate-schemas (fn [f]
                                          (binding [*config* (logic.migrations/make-config (->> (random-uuid)
                                                                                                str
                                                                                                (fs/join-path (System/getProperty "java.io.tmpdir"))))
                                                    *runner* (runners.mysql/make-mysql-migration-runner db-spec)]
                                            (f)

                                            (fs/rm! (:migrations-dir *config*)))))

(comment (deftest up-migration-test
           (testing "It should return nil if there are no migrations pending"
             ())))




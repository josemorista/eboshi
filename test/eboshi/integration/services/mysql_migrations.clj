(ns eboshi.integration.services.mysql-migrations
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [eboshi.services.migrations :as services.migrations]
            [eboshi.logic.migrations :as logic.migrations]
            [eboshi.logic.fs :as fs]
            [eboshi.infra.mysql-migration-runner :as runners.mysql]
            [next.jdbc :as jdbc])
  (:import [org.testcontainers.containers MySQLContainer]))


; Dummies
(def dummy-migrations [{:name "1Createt_1Table"
                        :up ["create table if not exists t_1(id int not null);"]
                        :down ["drop table t_1;"]}
                       {:name "2Createt_2Table"
                        :up ["create table if not exists t_2(id int not null);"]
                        :down ["drop table t_2;"]}])
; Dynamics
(def ^:dynamic *config* nil)
(def ^:dynamic *runner* nil)
(def ^:dynamic *db-spec* nil)

; Helper fns
(defn write-dummy-migrations []
  (doseq [migration dummy-migrations]
    (spit (fs/join-path (:migrations-dir *config*) (:name migration)) migration)))

(defn check-if-table-exists
  [con table-name]
  (-> table-name
      (#(jdbc/execute-one! con ["show tables like ?" %]))
      nil?
      not))

; Fixtures
(use-fixtures :once st/validate-schemas)
(use-fixtures :each
  (fn [f]
    (let [mysql-container (doto (MySQLContainer. "mysql:lts")
                            (.withDatabaseName "eboshi")
                            .start)
          db-spec {:jdbcUrl (.getJdbcUrl mysql-container)
                   :user (.getUsername mysql-container)
                   :password (.getPassword mysql-container)
                   :dbname "eboshi"}]
      (binding [*config* (logic.migrations/make-config (->> (random-uuid)
                                                            str
                                                            (fs/join-path (System/getProperty "java.io.tmpdir"))
                                                            (fs/assert-dir!)))
                *runner* (runners.mysql/make-mysql-migration-runner db-spec)
                *db-spec* db-spec]
        (try
          (f)
          (finally
            (.stop mysql-container)
            (fs/rm! (:migrations-dir *config*))))))))


(deftest up-migration-test
  (testing "It should return nil if there are no migrations pending"
    (is (nil? (services.migrations/up! *config* *runner*))))

  (testing "It should run up migrations in order"
    (write-dummy-migrations)
    (with-open [con (jdbc/get-connection *db-spec*)]
      (let [{:keys [name type instructions]} (services.migrations/up! *config* *runner*)]
        (is (= name (-> dummy-migrations first :name)))
        (is (= type :up))
        (is (= instructions (-> dummy-migrations first :up)))
        (is (check-if-table-exists con "t_1"))
        (is (not (check-if-table-exists con "t_2"))))

      (let [migration (services.migrations/up! *config* *runner*)]
        (is (= (:name migration) (-> dummy-migrations second :name)))
        (is (check-if-table-exists con "t_2"))))))

(deftest sync-migrations-test
  (testing "It should run all pending migrations"
    (write-dummy-migrations)
    (with-open [con (jdbc/get-connection *db-spec*)]
      (services.migrations/sync! *config* *runner*)
      (is (check-if-table-exists con "t_1"))
      (is (check-if-table-exists con "t_2")))))

(deftest down-migration-test
  (testing "It should revert migrations in reverse order"
    (write-dummy-migrations)
    (with-open [con (jdbc/get-connection *db-spec*)]
      (services.migrations/sync! *config* *runner*)
      (services.migrations/down! *config* *runner*)
      (is (false? (check-if-table-exists con "t_2")))
      (is (check-if-table-exists con "t_1"))
      (services.migrations/down! *config* *runner*)
      (is (false? (check-if-table-exists con "t_1"))))))


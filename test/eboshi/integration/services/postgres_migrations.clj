(ns eboshi.integration.services.postgres-migrations
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :as st]
            [eboshi.services.migrations :as services.migrations]
            [eboshi.logic.migrations :as logic.migrations]
            [eboshi.logic.fs :as fs]
            [eboshi.infra.postgres-migration-runner :as runners.postgres]
            [next.jdbc :as jdbc])
  (:import [org.testcontainers.containers PostgreSQLContainer]))


; Dummies
(def dummy-migrations [{:name "1_create_t1_table"
                        :up ["create table if not exists t_1(id int not null);"]
                        :down ["drop table if exists t_1;"]}
                       {:name "2_create_t2_table"
                        :up ["create table if not exists t_2(id int not null);"]
                        :down ["drop table if exists t_2;"]}])
; Dynamics
(def ^:dynamic *config* nil)
(def ^:dynamic *runner* nil)
(def ^:dynamic *db-spec* nil)

; Helper fns
(defn write-dummy-migrations []
  (doseq [migration dummy-migrations]
    (spit (fs/join-path (:migrations-dir *config*) (str (:name migration) ".edn")) migration)))

(defn check-if-table-exists
  [con table-name]
  (-> table-name
      (#(jdbc/execute-one! con ["select to_regclass(?) as regclass" %]))
      :regclass
      nil?
      not))

; Fixtures
(use-fixtures :once st/validate-schemas)
(use-fixtures :each
  (fn [f]
    (try
      (let [postgres-container (doto (PostgreSQLContainer. "postgres:16")
                                 (.withDatabaseName "eboshi")
                                 .start)
            db-spec {:jdbcUrl (.getJdbcUrl postgres-container)
                     :user (.getUsername postgres-container)
                     :password (.getPassword postgres-container)}]
        (binding [*config* (logic.migrations/make-config (->> (random-uuid)
                                                              str
                                                              (fs/join-path (System/getProperty "java.io.tmpdir"))
                                                              (fs/assert-dir!)))
                  *runner* (runners.postgres/make-postgres-migration-runner db-spec)
                  *db-spec* db-spec]
          (try
            (f)
            (finally
              (.stop postgres-container)
              (fs/rm! (:migrations-dir *config*))))))
      (catch IllegalStateException e
        (println "Skipping PostgreSQL integration tests because Docker is unavailable:" (.getMessage e))))))


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

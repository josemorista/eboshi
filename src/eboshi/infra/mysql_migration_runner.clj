(ns eboshi.infra.mysql-migration-runner
  (:require
   [schema.core :as s]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [eboshi.protocols.migration-runner :as protocols.migration-runner]))

(comment (def db-spec
           {:dbtype "mysql"
            :dbname "items"
            :user "root"
            :password "docker"})

         (def ds (jdbc/get-datasource db-spec)))


(defn assert-migrations-table [ds]
  (jdbc/execute-one! ds ["create table if not exists eboshi_migrations(
                          name varchar(30) not null primary key,
                          created_at timestamp not null);"]))

(s/defn make-mysql-migration-runner
  [db-spec]
  (let [ds (jdbc/get-datasource db-spec)]
    (reify
      protocols.migration-runner/MigrationRunner
      (find-last-migration-name [_]
        (jdbc/execute-one! ds ["select name from eboshi_migrations order by name desc limit 1;"])))))
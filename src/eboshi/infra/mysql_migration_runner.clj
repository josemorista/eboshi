(ns eboshi.infra.mysql-migration-runner
  (:require
   [schema.core :as s]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [eboshi.protocols.migration-runner :as protocols.migration-runner]
   [eboshi.models.migrations :as models.migrations]))

(defn with-assert-migrations-table!
  ([ds f args]
   (jdbc/execute-one! ds ["create table if not exists eboshi_migrations(
                          name varchar(30) not null primary key,
                          created_at timestamp not null);"])
   (apply f args))
  ([ds f]
   (with-assert-migrations-table! ds f [])))

(s/defn find-last-migration-name :- (s/maybe s/Str)
  [ds]
  (some-> (jdbc/execute-one! ds ["select name from eboshi_migrations order by name desc limit 1;"])
          :eboshi_migrations/name))

(s/defn execute!
  [ds migration :- models.migrations/Migration]
  (jdbc/with-transaction [tx ds]
    (doseq [instruction (:instructions migration)]
      (jdbc/execute! tx instruction))
    (sql/insert! tx :eboshi_migrations
                 {:name (:name migration)
                  :created_at (:created-at migration)})))

(s/defn make-mysql-migration-runner
  [db-spec]
  (let [ds (jdbc/get-datasource db-spec)
        with-migrations-table! (partial with-assert-migrations-table! ds)]
    (reify
      protocols.migration-runner/MigrationRunner
      (find-last-migration-name [_]
        (with-migrations-table! find-last-migration-name [ds])))))

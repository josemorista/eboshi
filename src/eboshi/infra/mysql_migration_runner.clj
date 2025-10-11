(ns eboshi.infra.mysql-migration-runner
  (:require
   [schema.core :as s]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [eboshi.protocols.migration-runner :as protocols.migration-runner]
   [eboshi.models.migrations :as models.migrations]))

(defn ^:private within-migrations-context!
  [db-spec & funcs]
  (with-open [con (jdbc/get-connection db-spec)]
    (jdbc/execute-one! con ["create table if not exists eboshi_migrations(
                              name varchar(30) not null primary key,
                              created_at timestamp not null);"])
    (mapv (fn [[f & args]]
            (-> f
                (partial con)
                (apply args))) funcs)))

(s/defn ^:private find-last-migration-name :- (s/maybe s/Str)
  [con]
  (some-> (jdbc/execute-one! con ["select name from eboshi_migrations order by name desc limit 1;"])
          :eboshi_migrations/name))

(s/defn ^:private execute!
  [con migration :- models.migrations/Migration]
  (jdbc/with-transaction [tx con]
    (doseq [stmt (:instructions migration)]
      (jdbc/execute! tx [stmt]))
    (case (:type migration)
      :up (sql/insert! tx :eboshi_migrations
                       {:name (:name migration)
                        :created_at (:created-at migration)})
      :down (sql/delete! con :eboshi_migrations {:name (:name migration)}))))

(s/defn make-mysql-migration-runner
  [db-spec]
  (reify
    protocols.migration-runner/MigrationRunner
    (find-last-migration-name [_self]
      (let [[last-migration] (within-migrations-context! db-spec [find-last-migration-name])]
        last-migration))
    (execute! [_self migration]
      (within-migrations-context! db-spec [execute! migration])
      migration)))

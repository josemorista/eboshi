(ns eboshi.services.migrations
  (:require
   [schema.core :as s]
   [eboshi.models.migrations :as models.migrations]
   [eboshi.logic.fs :as fs]
   [eboshi.protocols.migration-runner :as protocols.migration-runner]
   [eboshi.logic.migrations :as logic.migrations]
   [clojure.edn :as edn])
  (:import [java.util Date]))

(s/defn ^:private make-migration-from-file :- models.migrations/Migration
  [migration-name :- s/Str
   migrations-dir :- s/Str
   type :- models.migrations/MigrationType]
  (let [migration (edn/read-string (slurp (fs/join-path migrations-dir migration-name)))
        instructions (get migration type)]
    (logic.migrations/make-migration (:name migration) instructions type (Date.))))

(s/defn ^:private get-remaining-migrations-names
  [migrations-dir :- s/Str
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (let [cursor (protocols.migration-runner/find-last-migration-name runner)
        available-migrations (-> migrations-dir
                                 (fs/ls :short)
                                 sort)]
    (if (nil? cursor) available-migrations
        (filter #(> (compare % cursor) 0) available-migrations))))

(s/defn create [{:keys [migrations-dir]} :- models.migrations/MigrationsConfig migration-name :- s/Str]
  (let [migration-name-with-ts (str (-> (Date.) .getTime) "-" migration-name)]
    (-> migrations-dir
        fs/assert-dir!
        (fs/join-path (str migration-name-with-ts ".edn"))
        (spit {:name migration-name-with-ts :up [] :down []} :append true))))

(s/defn up! :- (s/maybe models.migrations/Migration)
  [{migrations-dir :migrations-dir} :- models.migrations/MigrationsConfig
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (when-first [migration (get-remaining-migrations-names migrations-dir runner)]
    (-> migration
        (make-migration-from-file migrations-dir :up)
        (#(protocols.migration-runner/execute! runner %)))))

(s/defn down! :- (s/maybe models.migrations/Migration)
  [{migrations-dir :migrations-dir} :- models.migrations/MigrationsConfig
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (when-let [cursor (protocols.migration-runner/find-last-migration-name runner)]
    (->> (make-migration-from-file cursor migrations-dir :down)
         (protocols.migration-runner/execute! runner))))

(s/defn sync! :- [models.migrations/Migration]
  [{migrations-dir :migrations-dir} :- models.migrations/MigrationsConfig
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (->> migrations-dir
       (#(get-remaining-migrations-names % runner))
       (mapv #(make-migration-from-file % migrations-dir :up))
       (mapv #(protocols.migration-runner/execute! runner %))))

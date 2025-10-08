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
  [migration-name :- [s/Str]
   instructions-dir :- s/Str
   type :- models.migrations/MigrationType]
  (let [migration (edn/read-string (slurp (fs/join-path instructions-dir migration-name)))
        instructions (get migration type)]
    (logic.migrations/make-migration migration-name instructions type (Date.))))


(s/defn ^:private get-remaining-migrations-names
  [migrations-dir :- s/Str
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (let [cursor (protocols.migration-runner/find-last-migration-name runner)
        available-migrations (-> migrations-dir
                                 (fs/ls :short)
                                 sort)]
    (if (nil? cursor) available-migrations
        (filter #(> % cursor) available-migrations))))

(s/defn ^:private run-next!
  [{migrations-dir :migrations-dir} :- models.migrations/MigrationsConfig
   runner :- protocols.migration-runner/MigrationRunnerProtocol
   type :- models.migrations/MigrationType]
  (let [[next-migration] (get-remaining-migrations-names migrations-dir runner)]
    (when-not (nil? next-migration)
      (-> next-migration
          (make-migration-from-file migrations-dir type)
          (protocols.migration-runner/execute runner)))))

(s/defn create [{:keys [migrations-dir]} :- models.migrations/MigrationsConfig migration-name :- s/Str]
  (let [migration-filename (str (-> (Date.) .getTime) migration-name ".edn")]
    (-> migrations-dir
        fs/assert-dir!
        (fs/join-path migration-filename)
        (spit {:name migration-filename :up [] :down []} :append true))))

(s/defn up! :- (s/maybe models.migrations/Migration)
  [config :- models.migrations/MigrationsConfig
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (run-next! config runner :up))

(s/defn down! :- (s/maybe models.migrations/Migration)
  [config :- models.migrations/MigrationsConfig
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (run-next! config runner :down))

(s/defn sync! :- [models.migrations/Migration]
  [{migrations-dir :migrations-dir} :- models.migrations/MigrationsConfig
   runner :- protocols.migration-runner/MigrationRunnerProtocol]
  (->> migrations-dir
       (#(get-remaining-migrations-names % runner))
       (map #(make-migration-from-file % migrations-dir :up))
       (map #(protocols.migration-runner/execute % runner))))
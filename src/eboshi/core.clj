(ns eboshi.core
  (:require
   [schema.core :as s]
   [eboshi.services.eboshi :as services.eboshi]
   [eboshi.models.eboshi :as models.eboshi]
   [eboshi.protocols.migration-runner :as protocols.migration-runner]
   [eboshi.infra.mysql-migration-runner :as runners.mysql]
   [eboshi.services.migrations :as services.migrations]
   [eboshi.logic.migrations :as logic.migrations]
   [eboshi.models.migrations :as models.migrations]
   [clojure.string :as str]))

(s/defn ^:private make-runner :- protocols.migration-runner/MigrationRunnerProtocol
  [runner :- models.eboshi/Runners
   spec]
  (case runner
    :mysql (runners.mysql/make-mysql-migration-runner spec)
    "default" (ex-info "Runner not available" {:runner runner})))

(s/defn ^:private load-eboshi-config
  [config-file-path :- (s/maybe s/Str)]
  (if config-file-path
    (services.eboshi/load-config config-file-path)
    (services.eboshi/load-config)))

(s/defn ^:private print-migrations-summary!
  [migrations :- [models.migrations/Migration]]
  (let [migrations-names (mapv :name migrations)]
    (println "Executed" (count migrations) "migrations:" (str/join "," migrations-names))))

(s/defn up!
  ([config-file-path :- (s/maybe s/Str)]
   (let [{:keys [runner spec] :as eboshi-config} (load-eboshi-config config-file-path)
         runner (make-runner runner spec)
         config (logic.migrations/make-config (-> eboshi-config :config :migrations-dir))]
     (->> (services.migrations/up! config runner)
          (conj [])
          print-migrations-summary!)))
  ([]
   (up! nil)))

(s/defn sync!
  ([config-file-path :- (s/maybe s/Str)]
   (let [{:keys [runner spec] :as eboshi-config} (load-eboshi-config config-file-path)
         runner (make-runner runner spec)
         config (logic.migrations/make-config (-> eboshi-config :config :migrations-dir))]
     (-> (services.migrations/sync! config runner)
         print-migrations-summary!)))
  ([]
   (sync! nil)))

(s/defn down!
  ([config-file-path :- (s/maybe s/Str)]
   (let [{:keys [runner spec] :as eboshi-config} (load-eboshi-config config-file-path)
         runner (make-runner runner spec)
         config (logic.migrations/make-config (-> eboshi-config :config :migrations-dir))]
     (->> (services.migrations/down! config runner)
          (conj [])
          print-migrations-summary!)))
  ([]
   (down! nil)))

(s/defn create!
  ([name :- s/Str
    config-file-path :- (s/maybe s/Str)]
   (let [eboshi-config (load-eboshi-config config-file-path)
         config (logic.migrations/make-config (-> eboshi-config :config :migrations-dir))]
     (services.migrations/create! config name)))
  ([name :- s/Str]
   (create! name nil)))

(s/defn init!
  ([runner :- s/Str
    config-file-path :- (s/maybe s/Str)]
   (services.eboshi/init! (keyword runner) config-file-path))
  ([runner :- s/Str]
   (services.eboshi/init! runner)))

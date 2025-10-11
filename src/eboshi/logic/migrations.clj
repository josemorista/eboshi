(ns eboshi.logic.migrations
  (:require
   [schema.core :as s]
   [eboshi.models.migrations :as models.migrations]))

(s/defn make-config :- models.migrations/MigrationsConfig
  [migrations-dir :- s/Str]
  {:migrations-dir migrations-dir})

(s/defn make-migration :- models.migrations/Migration
  [name :- s/Str
   instructions :- [s/Str]
   type :- models.migrations/MigrationType
   created-at :- s/Inst]
  {:name name :instructions instructions :type type :created-at created-at})

(s/defn env-config-value?
  [val]
  (and (keyword? val) (-> val str (.startsWith ":env/"))))

(s/defn parse-config-options
  [runner-spec]
  (->> runner-spec
       (mapv (fn [[key val]]
               (if (env-config-value? val)
                 [key (System/getProperty (name val))]
                 [key val])))
       (into {})))
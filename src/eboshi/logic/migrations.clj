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
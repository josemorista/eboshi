(ns eboshi.adapters.migrations
  (:require
   [schema.core :as s]
   [eboshi.models.migrations :as models.migrations]
   [eboshi.wires.mysql.migrations :as wires.mysql.migrations]))

(s/defn migration->mysql-migration :- eboshi.wires.mysql.migrations/Migration
  [{:keys [name created-at]} :- models.migrations/Migration]
  {:name name :created_at created-at})

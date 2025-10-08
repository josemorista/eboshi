(ns eboshi.wires.mysql.migrations
  (:require
   [schema.core :as s]))

(s/defschema Migration
  {:name s/Str
   :created_at s/Inst})

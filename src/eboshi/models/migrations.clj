(ns eboshi.models.migrations
  (:require
   [schema.core :as s]))

(s/defschema MigrationDialect (s/enum :sql))

(s/defschema MigrationsConfig
  {:migrations-dir s/Str})

(s/defschema MigrationType (s/enum :up :down))

(s/defschema Migration
  {:type MigrationType
   :name s/Str
   :created-at s/Inst
   :instructions s/Str})
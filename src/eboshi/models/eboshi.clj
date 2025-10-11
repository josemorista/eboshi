(ns eboshi.models.eboshi
  (:require
   [schema.core :as s]
   [eboshi.models.migrations :as models.migrations]))

(s/defschema Runners (s/enum :mysql))

(s/defschema EboshiConfig
  {:config models.migrations/MigrationsConfig
   :runner Runners
   :spec clojure.lang.PersistentArrayMap})
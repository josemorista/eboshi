(ns eboshi.services.eboshi
  (:require
   [schema.core :as s]
   [eboshi.models.eboshi :as models.eboshi]
   [eboshi.logic.migrations :as logic.migrations]
   [clojure.edn :as edn]))

(s/defn load-config :- models.eboshi/EboshiConfig
  ([config-file-path :- s/Str]
   (let [eboshi-config (edn/read-string (slurp config-file-path))
         spec (logic.migrations/parse-config-options (:spec eboshi-config))
         config (logic.migrations/make-config (:migrations-dir eboshi-config))]
     {:runner (:runner eboshi-config) :config config :spec spec}))
  ([]
   (load-config "eboshi.edn")))

(s/defn init!
  ([runner :- models.eboshi/Runners
    config-file-path :- s/Str]
   (spit config-file-path {:runner runner :migrations-dir "./migrations" :spec {}}))
  ([runner :- models.eboshi/Runners]
   (init! runner "eboshi.edn")))

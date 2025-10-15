(ns eboshi.services.eboshi
  (:require
   [schema.core :as s]
   [eboshi.models.eboshi :as models.eboshi]
   [eboshi.logic.migrations :as logic.migrations]
   [eboshi.logic.eboshi :as logic.eboshi]
   [clojure.edn :as edn]))

(s/defn load-config :- models.eboshi/EboshiConfig
  ([config-file-path :- s/Str]
   (let [{:keys [runner migrations-dir spec]} (-> (edn/read-string (slurp config-file-path))
                                                  logic.eboshi/parse-config)
         config (logic.migrations/make-config migrations-dir)]
     {:runner runner :config config :spec spec}))
  ([]
   (load-config "eboshi.edn")))

(s/defn init! :- s/Str
  ([runner :- models.eboshi/Runners
    config-file-path :- s/Str]
   (spit config-file-path {:runner runner :migrations-dir "./migrations" :spec {}})
   config-file-path)
  ([runner :- models.eboshi/Runners]
   (init! runner "eboshi.edn")))
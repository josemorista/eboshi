(ns eboshi.logic.eboshi
  (:require [schema.core :as s]))

(s/defn env-config-value?
  [val]
  (and (keyword? val) (-> val str (.startsWith ":env/"))))

(s/defn parse-config
  [runner-spec]
  (->> runner-spec
       (mapv (fn [[key val]]
               (cond
                 (env-config-value? val) [key (System/getProperty (name val))]
                 (map? val) [key (parse-config val)]
                 :else [key val])))
       (into {})))
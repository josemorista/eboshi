(ns eboshi.unit.adapters.mysql.migrations
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [eboshi.generators.migrations :as gen.migrations]
            [eboshi.adapters.migrations :as adapters.migrations]))


#_{:clj-kondo/ignore [:unresolved-symbol]}
(defspec migration->mysql-migration-test
  10
  (prop/for-all [migration gen.migrations/migration-gen]
                (let [mysql-migration (adapters.migrations/migration->mysql-migration migration)]
                  (are [model-key adapt-key]
                       (= (get migration model-key) (get mysql-migration adapt-key))
                    :name :name
                    :created-at :created_at))))


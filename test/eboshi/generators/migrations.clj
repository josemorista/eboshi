(ns eboshi.generators.migrations
  (:require [clojure.test.check.generators :as gen]))

(def migration-name-gen
  (gen/fmap
   #(str % "-" (gen/generate gen/string-alphanumeric))
   gen/nat))

(def migration-type-gen
  (gen/elements [:up :down]))

(def migration-instructions-gen
  (gen/vector (gen/elements ["create table if not exists users(id int not null primary key)"
                             "alter table users"]) 0 10))

(def migration-created-at-gen
  (gen/fmap #(java.util.Date. %) gen/nat))

(def migration-gen
  (gen/hash-map
   :name migration-name-gen
   :type migration-type-gen
   :instructions migration-instructions-gen
   :created-at migration-created-at-gen))

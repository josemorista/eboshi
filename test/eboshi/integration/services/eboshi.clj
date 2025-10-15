(ns eboshi.integration.services.eboshi
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [eboshi.services.eboshi :as services.eboshi]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.java.io :as io]))

(def runners-gen (gen/elements [:mysql]))

(def config-file-path-gen (gen/fmap
                           (fn [filename]
                             (-> filename
                                 (str "_.edn")
                                 (#(io/file (System/getProperty "java.io.tmpdir") %))
                                 .getAbsolutePath))
                           gen/string-alphanumeric))

(def config-spec-gen
  (gen/hash-map
   :user gen/string-alphanumeric
   :password gen/string-ascii))

(def config-gen
  (gen/hash-map
   :runner runners-gen
   :migrations-dir gen/string-alphanumeric
   :spec config-spec-gen))

(use-fixtures :once st/validate-schemas)

(defspec init!-test
  10
  (prop/for-all
   [runner runners-gen
    config-file-path config-file-path-gen]
   (is (-> (services.eboshi/init! runner config-file-path)
           io/as-file
           .exists))
   (println config-file-path runner)
   (io/delete-file config-file-path)))

(defspec load-config-test
  10
  (prop/for-all
   [config config-gen
    config-file-path config-file-path-gen]
   (let [pass-env-value (-> config :spec :password)
         pass-env-var "DUMMY_PASSWORD"
         config-with-env (assoc-in config [:spec :password] :env/DUMMY_PASSWORD)]
     (System/setProperty pass-env-var pass-env-value)
     (spit config-file-path config-with-env)
     (is (= {:runner (:runner config)
             :config {:migrations-dir (:migrations-dir config)}
             :spec {:user (-> config :spec :user)
                    :password pass-env-value}}
            (services.eboshi/load-config config-file-path))))))



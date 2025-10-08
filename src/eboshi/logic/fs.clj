(ns eboshi.logic.fs
  (:require [schema.core :as s]
            [clojure.java.io :as io]))

(s/defn join-path :- s/Str
  [& args :- [s/Str]]
  (str (apply io/file args)))

(s/defn is-dir? :- s/Bool
  [path :- s/Str]
  (let [f (io/file path)]
    (and (.exists f) (.isDirectory f))))

(s/defn ls :- [s/Str]
  ([path :- s/Str]
   (->> path
        io/as-file
        .listFiles
        (map str)))
  ([path :- s/Str mode :- (s/enum :short :full)]
   (let [files (ls path)]
     (case mode
       :short (->> files (map #(-> % io/as-file .getName)))
       files))))

(s/defn assert-dir! :- s/Str
  [path :- s/Str]
  (when-not (is-dir? path)
    (.mkdirs (io/as-file path)))
  path)

(s/defn rm!
  [path :- s/Str]
  (when (is-dir? path)
    (doseq [subpath (ls path)]
      (rm! subpath)))
  (io/delete-file path))



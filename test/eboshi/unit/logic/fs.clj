(ns eboshi.unit.logic.fs
  (:require [schema.core :as s]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [eboshi.logic.fs :as fs]))

(s/defn make-dummy-file!
  [dir :- s/Str filename :- s/Str]
  (let [file-path (str dir "/" filename)]
    (spit file-path (random-uuid))
    file-path))

(s/defn make-dummy-dir! :- s/Str
  []
  (let [tmpdir (-> (System/getProperty "java.io.tmpdir")
                   (io/file (str (random-uuid))))]
    (.mkdir tmpdir)
    (str tmpdir)))

(deftest rm!-test
  (testing "It should remove a full dir"
    (let [dummy-dir (make-dummy-dir!)]
      (doto dummy-dir
        (make-dummy-file! "eboshi-1.txt")
        (make-dummy-file! "eboshi-2.txt"))
      (-> dummy-dir
          fs/rm!)
      (is (false? (.exists (io/as-file dummy-dir)))))))

(deftest ls-test
  (testing "It should list all files of directory"
    (let [dummy-dir (make-dummy-dir!)
          dummy-file (make-dummy-file! dummy-dir "eboshi.txt")
          [file] (fs/ls dummy-dir)]
      (is (= dummy-file file))
      (fs/rm! dummy-dir)))

  (testing "When in :short mode, should list all file names of directory"
    (let [dummy-dir (make-dummy-dir!)
          _ (make-dummy-file! dummy-dir "eboshi-2.txt")
          [file] (fs/ls dummy-dir :short)]
      (is (= "eboshi-2.txt" file))
      (fs/rm! dummy-dir))))

(deftest join-path
  (testing "It should join relative paths"
    (is (= "../tmp/dir/eboshi.txt" (fs/join-path ".." "tmp" "dir" "eboshi.txt")))))

(deftest is-dir?-test
  (testing "Should return false if path does not exists")
  (is (false? (fs/is-dir? "random.txt")))

  (testing "Should return false if path is a file")
  (let [dummy-dir (make-dummy-dir!)
        dummy-file (make-dummy-file! dummy-dir "eboshi.txt")]
    (is (false? (fs/is-dir? dummy-file)))
    (fs/rm! dummy-dir))

  (testing "Should return true if is a dir"
    (let [dummy-dir (make-dummy-dir!)]
      (is (fs/is-dir? dummy-dir))
      (fs/rm! dummy-dir))))

(deftest assert-dir!-test
  (testing "Should create dir if not exists"
    (let [dummy-dir-path (-> (System/getProperty "java.io.tmpdir")
                             (str "/" (random-uuid)))
          dummy-dir (io/as-file dummy-dir-path)]
      (fs/assert-dir! dummy-dir-path)
      (is (and (.exists dummy-dir) (.isDirectory dummy-dir)))
      (fs/rm! dummy-dir))))
(defproject org.clojars.josemorista/eboshi "0.2.0"
  :description "Eboshi is a lightweight Clojure library for managing SQL migrations stored as EDN files and 
                executing them against a relational database. The project provides a migration-runner protocol and a MySQL implementation using next.jdbc."
  :url "https://github.com/josemorista/eboshi"
  :release-tasks [["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]]
  :repositories [["releases" {:sign-releases false}]]
  :deploy-repositories {"clojars" {:sign-releases false
                                   :username :env/CLOJARS_USERNAME
                                   :password :env/CLOJARS_TOKEN}}
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [prismatic/schema "1.4.1"]
                 [com.github.seancorfield/next.jdbc "1.3.1070"]
                 [com.mysql/mysql-connector-j "9.4.0"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]
                                  [org.testcontainers/mysql "1.21.3"]]
                   :source-paths ["src" "test"]}
             :uberjar {:aot [:all]}}
  :aliases {"test" ["with-profile" "+dev" "test"]
            "deploy:clojars" ["with-profile" "+uberjar" "deploy" "clojars"]}
  :main eboshi.core
  :repl-options {:init-ns eboshi.core})

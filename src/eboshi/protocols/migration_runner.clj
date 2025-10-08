(ns eboshi.protocols.migration-runner
  (:require
   [schema.core :as s]
   [eboshi.models.migrations :as models.migrations]))

(s/defprotocol MigrationRunner
  "Migration runner protocol"
  (find-last-migration-name :- (s/maybe s/Str) [self])
  (execute :- models.migrations/Migration [migration :- models.migrations/Migration self]))

(def MigrationRunnerProtocol (s/protocol MigrationRunner))
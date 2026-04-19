[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.josemorista/eboshi.svg)](https://clojars.org/org.clojars.josemorista/eboshi)

# eboshi

Eboshi is a lightweight Clojure library for managing SQL migrations stored as EDN files and executing them against a relational database.
The project provides a migration-runner protocol with MySQL and Postgres implementations using next.jdbc.

## Key concepts

- Migrations are represented as EDN files with a map shape:

```edn
{:name "1600000000000-create-users"
 :up   ["create table if not exists users (id int auto_increment primary key, name varchar(255));"]
 :down ["drop table if exists users;"]}
```

- Files are ordered by filename (timestamp prefix) and the `:name` field is used to track applied migrations.
- The `MigrationRunner` protocol declares two operations:
  - `find-last-migration-name` — returns the last applied migration name (maybe String)
  - `execute!` — apply or revert a migration and return the migration map

## Files of interest

- `src/eboshi/services/migrations.clj` — high-level migration helpers (create, up!, down!, sync!)
- `src/eboshi/protocols/migration_runner.clj` — `s/defprotocol` for migration runners (Schema is used for runtime validation)
- `src/eboshi/infra/mysql_migration_runner.clj` — MySQL runner implementation using `next.jdbc`
- `src/eboshi/infra/postgres_migration_runner.clj` — Postgres runner implementation using `next.jdbc`
- `test/...` — unit and integration tests. Integration tests use Testcontainers (`MySQLContainer` and `PostgreSQLContainer`) and require Docker.

## Usage examples

Create a database spec for a local MySQL or Postgres instance (ensure the matching JDBC driver is in your deps):

```clojure
;; URL string form
(def db-spec "jdbc:mysql://localhost:3306/eboshi?useSSL=false&serverTimezone=UTC&user=root&password=secret")

;; or map form
(def db-spec {:jdbcUrl  "jdbc:mysql://localhost:3306/eboshi?useSSL=false&serverTimezone=UTC"
							:user     "root"
							:password "secret"})
```

Create and use a migration runner:

```clojure
(require '[eboshi.infra.mysql-migration-runner :as mysql-runner]
         '[eboshi.infra.postgres-migration-runner :as postgres-runner]
				 '[eboshi.services.migrations :as migrations])

(def runner (mysql-runner/make-mysql-migration-runner db-spec))
;; or (def runner (postgres-runner/make-postgres-migration-runner db-spec))
(def config {:migrations-dir "/path/to/migrations"})

;; write a migration (helper provided by the library)
(migrations/create config "add-users")

;; apply next pending migration
(migrations/up! config runner)

;; apply all pending migrations
(migrations/sync! config runner)

;; revert last migration
(migrations/down! config runner)
```

## Using eboshi from a config file (core namespace)

You can drive eboshi directly from a configuration EDN file by calling the functions in the `eboshi.core` namespace. This is useful when running from a REPL, embedding eboshi in scripts, or invoking it from other tooling.

Example config file (`eboshi.edn`):

```edn
{:runner :mysql
 :spec   "jdbc:mysql://localhost:3306/eboshi?useSSL=false&serverTimezone=UTC&user=root&password=secret"
 :config {:migrations-dir "./migrations"}}
```

Notes:

- `:runner` should match a supported runner keyword (for example `:mysql` or `:postgres`).
- `:spec` can be a JDBC URL string or a map, depending on your runner implementation.
- `:config` should include `:migrations-dir` pointing to your migrations folder.

Call from a REPL:

```clojure
(require '[eboshi.core :as eboshi.core])

;; apply next pending migration using the config file
(eboshi.core/up!)

;; apply all pending migrations
(eboshi.core/sync!)

;; revert last migration
(eboshi.core/down!)

;; create a new migration file (writes into :migrations-dir defined in the config)
(eboshi.core/create! "add-users")
```

Default config behavior:

- If you omit the `config-file-path`, the core functions call the library's default config loader (see `eboshi.services.eboshi/load-config`). Pass a path when you need to use a specific EDN file.

Examples in scripts:

- From a script or automation that can run Clojure code, require `eboshi.core` and call the desired function with the path to your EDN config file.

## Developer notes

### Running tests

The project includes unit and integration tests. Integration tests use Testcontainers and require Docker to be available.

Using Leiningen:

```bash
lein test
```

If you run only unit tests or want to skip integration tests, use your usual tooling/aliases as configured in the project.

### Extending and contributing

- To add another backend, implement the `eboshi.protocols.migration-runner/MigrationRunner` protocol.
- Keep migration files valid EDN. Avoid embedding functions or non-standard tagged literals.
- Add tests for new behavior and follow existing patterns in `test/`.

## License

See `LICENSE` in the project root.

# eboshi

Eboshi is a lightweight Clojure library for managing SQL migrations stored as EDN files and executing them against a relational database. The project provides a migration-runner protocol and a MySQL implementation using next.jdbc.

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
- `test/...` — unit and integration tests. Integration tests use Testcontainers' `MySQLContainer` and require Docker.

## Usage examples

Create a database spec for a local MySQL instance (ensure MySQL driver is in your deps):

```clojure
;; URL string form
(def db-spec "jdbc:mysql://localhost:3306/eboshi?useSSL=false&serverTimezone=UTC&user=root&password=secret")

;; or map form
(def db-spec {:jdbcUrl  "jdbc:mysql://localhost:3306/eboshi?useSSL=false&serverTimezone=UTC"
							:user     "root"
							:password "secret"})
```

Create and use the MySQL migration runner:

```clojure
(require '[eboshi.infra.mysql-migration-runner :as mysql-runner]
				 '[eboshi.services.migrations :as migrations])

(def runner (mysql-runner/make-mysql-migration-runner db-spec))
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

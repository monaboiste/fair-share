import sqlite3
import subprocess
import unittest
from contextlib import closing
from pathlib import Path
from tempfile import TemporaryDirectory
from uuid import uuid4


ROOT = Path(__file__).resolve().parents[4]


class MigrationTaskTest(unittest.TestCase):
    def migrate(self, database=None, error=None, options=()):
        command = [str(ROOT / "gradlew"), ":infrastructure:migrateDatabase"]
        if database is not None:
            command.append(f"-PdatabasePath={database}")
        result = subprocess.run(
            [*command, *options], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True
        )
        if error is None:
            self.assertEqual(result.returncode, 0, result.stdout)
        else:
            self.assertNotEqual(result.returncode, 0, result.stdout)
            self.assertIn(error, result.stdout)

    def test_requires_an_explicit_absolute_path(self):
        self.migrate(error="Set -PdatabasePath to an absolute SQLite database file path")
        relative = Path(f"relative-{uuid4()}.db")
        self.migrate(relative, error="Set -PdatabasePath to an absolute SQLite database file path")
        self.assertFalse((ROOT / relative).exists())
        self.assertFalse((ROOT / "infrastructure" / relative).exists())

    def test_liquibase_url_cannot_redirect_the_migration(self):
        with TemporaryDirectory() as directory:
            intended = Path(directory) / "intended.db"
            unintended = Path(directory) / "unintended.db"
            self.migrate(intended, error="Use -PdatabasePath instead of -PliquibaseUrl",
                         options=[f"-PliquibaseUrl=jdbc:sqlite:{unintended}"])
            self.assertFalse(intended.exists())
            self.assertFalse(unintended.exists())

    def test_provisions_new_and_empty_files(self):
        with TemporaryDirectory() as directory:
            for existing in (False, True):
                with self.subTest(existing=existing):
                    path = Path(directory) / f"events {existing}#.db?busy_timeout=123"
                    if existing:
                        path.touch()

                    self.migrate(path)
                    with closing(sqlite3.connect(path.as_uri() + "?mode=rw", uri=True)) as db:
                        self.assertEqual(db.execute("PRAGMA user_version").fetchone(), (1,))
                        tables = db.execute(
                            "SELECT name, strict FROM pragma_table_list WHERE name IN ('event_stream', 'event') ORDER BY name"
                        ).fetchall()
                        self.assertEqual(tables, [("event", 1), ("event_stream", 1)])
                        self.assertEqual(
                            db.execute("PRAGMA foreign_key_list(event)").fetchone()[2:5],
                            ("event_stream", "stream_id", "stream_id"),
                        )
                        self.assertEqual(
                            db.execute("SELECT name, type, [notnull], pk FROM pragma_table_info('event_stream')").fetchall(),
                            [("stream_id", "TEXT", 1, 1), ("revision", "INTEGER", 1, 0), ("last_sequence", "INTEGER", 1, 0)],
                        )
                        self.assertEqual(
                            db.execute("SELECT name, type, [notnull], pk FROM pragma_table_info('event')").fetchall(),
                            [
                                ("event_id", "TEXT", 1, 0),
                                ("stream_id", "TEXT", 1, 1),
                                ("sequence", "INTEGER", 1, 2),
                                ("revision", "INTEGER", 1, 0),
                                ("recorded_at", "TEXT", 1, 0),
                                ("event_type", "TEXT", 1, 0),
                                ("event_version", "INTEGER", 1, 0),
                                ("payload", "TEXT", 1, 0),
                            ],
                        )

    def test_enforces_event_schema_checks(self):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "events.db"
            self.migrate(path)
            with closing(sqlite3.connect(path)) as db, db:
                db.execute("INSERT INTO event_stream VALUES ('s1', 1, 1)")
                for revision, last_sequence in [(0, 1), (2, 1)]:
                    with self.subTest(revision=revision, last_sequence=last_sequence):
                        with self.assertRaisesRegex(sqlite3.IntegrityError, "CHECK constraint failed"):
                            db.execute("INSERT INTO event_stream VALUES ('s2', ?, ?)", (revision, last_sequence))

                for revision, event_type, event_version in [
                    (0, "group-created", 1),
                    (2, "group-created", 1),
                    (1, "", 1),
                    (1, "group-created", 0),
                    (1, "group-created", 2147483648),
                ]:
                    with self.subTest(revision=revision, event_type=event_type, event_version=event_version):
                        with self.assertRaisesRegex(sqlite3.IntegrityError, "CHECK constraint failed"):
                            db.execute(
                                "INSERT INTO event VALUES ('e1', 's1', 1, ?, '2026-01-01T00:00:00Z', ?, ?, '{}')",
                                (revision, event_type, event_version),
                            )

    def test_repeat_preserves_events_stream_revisions_and_schema_version(self):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "events.db"
            fresh_daemon = ("--no-daemon",)
            self.migrate(path, options=fresh_daemon)
            stream = ("s1", 1, 1)
            event = ("e1", "s1", 1, 1, "2026-01-01T00:00:00Z", "group-created", 1, "{}")
            with closing(sqlite3.connect(path)) as db, db:
                db.executemany("INSERT INTO event_stream VALUES (?, ?, ?)", [stream, ("s2", 1, 1)])
                db.execute("INSERT INTO event VALUES (?, ?, ?, ?, ?, ?, ?, ?)", event)
                with self.assertRaises(sqlite3.IntegrityError):
                    db.execute("INSERT INTO event VALUES (?, 's2', ?, ?, ?, ?, ?, ?)", event[:1] + event[2:])

            self.migrate(path, options=fresh_daemon)
            with closing(sqlite3.connect(path)) as db:
                self.assertEqual(db.execute("PRAGMA user_version").fetchone(), (1,))
                self.assertEqual(
                    db.execute("SELECT * FROM event_stream ORDER BY stream_id").fetchall(), [stream, ("s2", 1, 1)]
                )
                self.assertEqual(db.execute("SELECT * FROM event").fetchall(), [event])

    def test_either_unmanaged_event_table_blocks_provisioning(self):
        with TemporaryDirectory() as directory:
            for table in ("event", "event_stream"):
                with self.subTest(table=table):
                    path = Path(directory) / f"{table}.db"
                    ddl = f"CREATE TABLE {table} (preserve TEXT)"
                    with closing(sqlite3.connect(path)) as db, db:
                        db.execute(ddl)
                        db.execute(f"INSERT INTO {table} VALUES ('existing')")
                        db.execute("PRAGMA user_version = 7")

                    self.migrate(path, error="Unmanaged event tables already exist")
                    with closing(sqlite3.connect(path)) as db:
                        self.assertEqual(db.execute("PRAGMA user_version").fetchone(), (7,))
                        self.assertEqual(db.execute(f"SELECT * FROM {table}").fetchall(), [("existing",)])
                        self.assertEqual(
                            db.execute("SELECT sql FROM sqlite_schema WHERE name IN ('event_stream', 'event')").fetchall(),
                            [(ddl,)],
                        )

    def test_failed_schema_creation_leaves_no_partial_schema_or_new_version(self):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "events.db"
            with closing(sqlite3.connect(path)) as db, db:
                db.execute("CREATE VIEW event AS SELECT 1")

            self.migrate(path, error="view event already exists")
            with closing(sqlite3.connect(path)) as db:
                self.assertEqual(db.execute("PRAGMA user_version").fetchone(), (0,))
                self.assertEqual(
                    db.execute("SELECT name, type FROM sqlite_schema WHERE name IN ('event_stream', 'event')").fetchall(),
                    [("event", "view")],
                )


if __name__ == "__main__":
    unittest.main()

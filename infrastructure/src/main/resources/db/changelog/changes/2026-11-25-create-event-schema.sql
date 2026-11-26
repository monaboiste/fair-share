--liquibase formatted sql

--changeset monaboiste:2026-11-25-create-event-schema dbms:sqlite runInTransaction:true
--preconditions onFail:HALT onError:HALT onFailMessage:Unmanaged event tables already exist. Use a new or empty SQLite database.
--precondition-sql-check expectedResult:0 PRAGMA user_version
--precondition-sql-check expectedResult:0 SELECT count(*) FROM sqlite_schema WHERE type = 'table' AND name COLLATE NOCASE = 'event_stream'
--precondition-sql-check expectedResult:0 SELECT count(*) FROM sqlite_schema WHERE type = 'table' AND name COLLATE NOCASE = 'event'

CREATE TABLE event_stream (
    stream_id TEXT NOT NULL PRIMARY KEY,
    revision INTEGER NOT NULL CHECK (revision > 0),
    last_sequence INTEGER NOT NULL CHECK (last_sequence >= revision)
) STRICT;

CREATE TABLE event (
    event_id TEXT NOT NULL UNIQUE,
    stream_id TEXT NOT NULL REFERENCES event_stream (stream_id),
    sequence INTEGER NOT NULL CHECK (sequence > 0),
    revision INTEGER NOT NULL CHECK (revision > 0 AND revision <= sequence),
    recorded_at TEXT NOT NULL,
    event_type TEXT NOT NULL CHECK (length(event_type) > 0),
    event_version INTEGER NOT NULL CHECK (event_version BETWEEN 1 AND 2147483647),
    payload TEXT NOT NULL,
    PRIMARY KEY (stream_id, sequence)
) STRICT;

PRAGMA user_version = 1;

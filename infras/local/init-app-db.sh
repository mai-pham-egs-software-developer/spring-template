#!/bin/bash
# Runs once, only when the postgres container's data volume is first initialized (official
# postgres image convention: every *.sh/*.sql under /docker-entrypoint-initdb.d/ runs on first
# boot of an empty data dir, never again after). Creates the separate `app` database that
# applications/main's spring.datasource.url points at, alongside the `keycloak` database created
# by POSTGRES_DB -- keeps file-storage's stored_file table out of Keycloak's own schema.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE app;
EOSQL

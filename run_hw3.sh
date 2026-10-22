#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
DATA=${1:-"$ROOT/ml-1m"}
HBASE_BIN=${HBASE_BIN:-hbase}
command -v "$HBASE_BIN" >/dev/null
command -v javac >/dev/null
command -v python3 >/dev/null
if [[ ! -d "$DATA" && "$DATA" == "$ROOT/ml-1m" ]]; then
    unzip -q "$ROOT/ml-1m.zip" -d "$ROOT"
fi
DATA=$(cd -- "$DATA" && pwd)
for file in ratings.dat movies.dat users.dat; do test -s "$DATA/$file"; done
mkdir -p "$ROOT/build"
CP=$("$HBASE_BIN" classpath)
javac -encoding UTF-8 -cp "$CP" -d "$ROOT/build" "$ROOT/LoadData.java" "$ROOT/Problem1.java" "$ROOT/Problem2.java" "$ROOT/Problem3.java"
cd "$ROOT"
printf 'Resetting movielens_ratings and reloading the dataset...\n'
"$HBASE_BIN" shell -n "$ROOT/CreateTable.hbase" > part_1_create_output.txt 2> hbase_shell_errors.log
java -cp "$ROOT/build:$CP" LoadData "$DATA" > part_1_load_output.txt
"$HBASE_BIN" shell -n "$ROOT/VerifyTable.hbase" > part_1_schema_samples_output.txt 2>> hbase_shell_errors.log
java -cp "$ROOT/build:$CP" Problem1 > part_1_invalid_output.txt
java -cp "$ROOT/build:$CP" Problem2
"$HBASE_BIN" shell -n "$ROOT/VersionTest.hbase" > part_2_versions_shell_output.txt 2>> hbase_shell_errors.log
"$HBASE_BIN" shell -n "$ROOT/HBaseFilters.hbase" > part_2_filters_shell_output.txt 2>> hbase_shell_errors.log
java -cp "$ROOT/build:$CP" Problem3
bash "$ROOT/package_submission.sh"

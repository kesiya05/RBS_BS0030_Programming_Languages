#!/usr/bin/env bash
# Verify the BS0030 functional Codespace environment used in Weeks 3-4.
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

for tool in git java clojure clj; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        printf 'MISSING: %s\n' "$tool" >&2
        exit 1
    fi
done

java_line="$(java -version 2>&1 | head -n 1)"
java_major="$(printf '%s\n' "$java_line" | sed -E 's/.*version "([0-9]+).*/\1/')"

if [[ "$java_major" != "25" ]]; then
    printf 'Expected Java 25; found: %s\n' "$java_line" >&2
    exit 1
fi

printf 'BS0030 Functional Environment\n'
printf 'Git: %s\n' "$(git --version)"
printf 'Java: %s\n' "$java_line"
printf 'Clojure CLI:\n'
clojure -Sdescribe

cd "$ROOT/labs/week03"

printf 'Preparing Week 3 dependencies...\n'
clojure -P

language_version="$(clojure -M -e '(print (clojure-version))')"
if [[ "$language_version" != "1.12.6" ]]; then
    printf 'Expected Clojure language 1.12.6 from labs/week03/deps.edn; found %s\n' "$language_version" >&2
    exit 1
fi

clojure -M -e '(assert (= 14 (+ 2 (* 3 4)))) (println "Clojure evaluation: OK")'

printf 'Clojure language: %s\n' "$language_version"
printf '\nEnvironment ready.\n'
printf 'Week 3: labs/week03/README.md\n'

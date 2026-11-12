#!/usr/bin/env bash
# Compiles and runs the pure-Kotlin parts (core-lik31 + the app's UI-free state
# logic) WITHOUT Gradle dependency resolution, using the Kotlin compiler and
# JUnit that ship inside a Gradle distribution. Handy on machines that cannot
# reach Maven Central. The normal way to test is: ./gradlew test
set -euo pipefail
GRADLE_HOME="${GRADLE_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v gradle)")")")}"
LIB="$GRADLE_HOME/lib"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${OUT:-$(mktemp -d)}"

KOTLINC_CP="$(ls "$LIB"/kotlin-compiler-embeddable-*.jar "$LIB"/kotlin-stdlib-*.jar "$LIB"/kotlin-script-runtime-*.jar "$LIB"/kotlin-reflect-*.jar "$LIB"/kotlin-daemon-embeddable-*.jar "$LIB"/trove4j-*.jar "$LIB"/annotations-*.jar "$LIB"/kotlinx-coroutines-core-jvm-*.jar | tr '\n' ':')"
STDLIB="$(ls "$LIB"/kotlin-stdlib-*.jar)"
JUNIT="$(ls "$LIB"/junit-4*.jar):$(ls "$LIB"/hamcrest-core-*.jar)"

SOURCES=("$ROOT/core-lik31/src/main/kotlin" "$ROOT/core-lik31/src/test/kotlin")
# UI-free app sources: every file that imports nothing from android.* / androidx.*
while IFS= read -r f; do SOURCES+=("$f"); done < <(grep -L -r -E '^import androidx?\.' "$ROOT/app/src/main/kotlin" --include='*.kt' || true)
[ -d "$ROOT/app/src/test/kotlin" ] && SOURCES+=("$ROOT/app/src/test/kotlin")

java -cp "$KOTLINC_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -cp "$STDLIB:$JUNIT" -jvm-target 17 -d "$OUT/classes" "${SOURCES[@]}"

TESTS=$(cd "$OUT/classes" && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g' | sort | tr '\n' ' ')
# shellcheck disable=SC2086
java -cp "$OUT/classes:$ROOT/core-lik31/src/test/resources:$STDLIB:$JUNIT" org.junit.runner.JUnitCore $TESTS

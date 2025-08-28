#!/usr/bin/env bash
set -euo pipefail

command -v java >/dev/null 2>&1 || { echo "Java not found. Install OpenJDK 17+."; exit 1; }

# soft version hint (keep it short)
if ! java -version 2>&1 | grep -Eq '"1[7-9]|"2[0-9]'; then
  echo "Warning: Java 17+ recommended."
fi

# rw check for current dir
touch .__rw_check__ && rm -f .__rw_check__ || { echo "No write permission in $(pwd)."; exit 1; }

# color/ANSI hint
if ! { [ -t 1 ] && command -v tput >/dev/null 2>&1 && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; }; then
  echo "Note: Limited ANSI color detected — this app looks best in a color-capable terminal."
fi

# --- build ---
### remove the -q from the below line if you want to see the build log
set +e
./mvnw -q -DskipTests package 2>&1 | tee -a session.log
status=${PIPESTATUS[0]}
set -e
[ "$status" -eq 0 ] || { echo "Build failed"; exit "$status"; }

# --- pick jar (newest runnable jar) ---
JAR_PATH="${JAR_PATH:-}"
if [ -z "${JAR_PATH}" ]; then
  JAR_PATH="$(ls -t target/*.jar 2>/dev/null | grep -Ev 'sources|javadoc|tests|original' | head -n1 || true)"
fi
[ -n "${JAR_PATH}" ] && [ -f "${JAR_PATH}" ] || { echo "No runnable JAR found in target/. Did the build produce a jar?"; exit 1; }

echo "Launching: ${JAR_PATH}"
# stream output to console + both logs
java -jar "${JAR_PATH}" "$@"

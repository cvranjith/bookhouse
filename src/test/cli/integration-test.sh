#!/usr/bin/env bash
set -u

JAR="${1:-target/bookhouse-0.0.1-SNAPSHOT.jar}"

OUT="$(mktemp)"
trap 'rm -f "$OUT"' EXIT

echo "Running integration test with JAR: $JAR"

set +e

java -jar "$JAR" \
    --spring.output.ansi.enabled=NEVER \
    --logging.level.root=ERROR \
    > "$OUT" 2>&1 << 'EOF'
login admin
add-book test
add-book 'Autobiography of Ranjith'
list
logout
login user
waitlist test
borrow test
status
logout
exit
EOF

APP_EXIT_CODE=$?

# Clean ANSI color codes from the output
CLEAN_OUT="$(mktemp)"
trap 'rm -f "$OUT" "$CLEAN_OUT"' EXIT
sed 's/\x1b\[[0-9;]*m//g' "$OUT" > "$CLEAN_OUT"

cat "$CLEAN_OUT"

# --helper funcitons
EXPECT_PASSES=0
EXPECT_FAILS=0

expected_output() {
  local NEEDLE="$1"
  local FILE="${2:-$CLEAN_OUT}"
  if grep -Fqi -- "$NEEDLE" "$FILE" >/dev/null 2>&1; then
    echo "✅ FOUND: '$NEEDLE'"
    EXPECT_PASSES=$((EXPECT_PASSES + 1))
  else
    echo "❌ MISSING: '$NEEDLE'"
    EXPECT_FAILS=$((EXPECT_FAILS + 1))
  fi
}

echo "=== Starting Assertions ==="
expected_output "Welcome, admin! You are now logged In"
expected_output "Added 1 copy(ies) of Book \"test\""
expected_output "Added 1 copy(ies) of Book \"Autobiography of Ranjith\""
expected_output "Welcome, user! You are now logged In"
expected_output "Succesfully joined the waitlist for book \"test\". Your position is 1"
expected_output "Successuflly borrowed the book \"test\""


echo ""
echo "=== Assertion Results ==="
echo "✅ Passes: $EXPECT_PASSES"
echo "❌ Fails: $EXPECT_FAILS"


if [ "$EXPECT_FAILS" -gt 0 ]; then
    echo ""
    echo "❌ FINAL RESULT: ${EXPECT_FAILS} failed, ${EXPECT_PASSES} passed"
else
    echo ""
    echo "✅ FINAL RESULT: All ${EXPECT_PASSES} checks passed!"
fi

if [ "$APP_EXIT_CODE" -ne 0 ]; then
    echo "⚠️  App exited with non-zero code: $APP_EXIT_CODE"
fi

if [ "$APP_EXIT_CODE" -ne 0 ] && [ "$EXPECT_FAILS" -gt 0 ]; then
    echo "***EXIT WITH ERROR**"
    exit 1
fi
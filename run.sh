#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "=== Cleaning previous build artifacts ==="
rm -rf bin
mkdir -p bin

echo "=== Finding all Java source files ==="
find src -name "*.java" > sources.txt

echo "=== Compiling Java source files ==="
javac -d bin @sources.txt

echo "=== Cleaning up temporary sources list ==="
rm sources.txt

echo "=== Running LedgerRunner ==="
java -cp bin mal.ledger.LedgerRunner
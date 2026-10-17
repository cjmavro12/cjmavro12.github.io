#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p target/core-checks
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d target/core-checks src/main/java/com/snhu/merkle/MerkleTree.java src/main/java/com/snhu/merkle/MerkleDemo.java src/test/java/com/snhu/merkle/MerkleCoreChecks.java
java -cp target/core-checks com.snhu.merkle.MerkleCoreChecks
java -cp target/core-checks com.snhu.merkle.MerkleDemo

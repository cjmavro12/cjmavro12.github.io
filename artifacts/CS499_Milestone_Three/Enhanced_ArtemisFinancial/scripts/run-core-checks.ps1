$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
New-Item -ItemType Directory -Force target/core-checks | Out-Null
& java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d target/core-checks src/main/java/com/snhu/merkle/MerkleTree.java src/main/java/com/snhu/merkle/MerkleDemo.java src/test/java/com/snhu/merkle/MerkleCoreChecks.java
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed. Use JDK 17 or newer." }
& java -cp target/core-checks com.snhu.merkle.MerkleCoreChecks
if ($LASTEXITCODE -ne 0) { throw "Core checks failed" }
& java -cp target/core-checks com.snhu.merkle.MerkleDemo
if ($LASTEXITCODE -ne 0) { throw "Demo failed" }

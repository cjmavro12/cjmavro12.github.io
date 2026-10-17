# Artemis Financial Merkle Tree Enhancement

This CS 499 Milestone Three version extends the CS 305 SHA-256 checksum project with ordered record verification. The original `/hash?data=...` endpoint remains. MerkleTree is plain Java; the web controllers call it without duplicating its algorithm.

## Open it on Windows

1. Extract the submission ZIP into a new folder, for example `G:\CS499\MilestoneThree`.
2. Open IntelliJ IDEA, choose **Open**, and select this `Enhanced_ArtemisFinancial` folder containing `pom.xml`. In VS Code, choose **File > Open Folder** and select the same folder.
3. Use **JDK 17 or newer**. Run `java -version` in the terminal to check. Set the IDE's project JDK and Maven runner JDK to the same installation. An Android Studio bundled JDK can also run the core script if its `bin` folder is on PATH.
4. Open the terminal in this folder. Run the core test and demo without downloading Maven dependencies:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-core-checks.ps1
```

The policy option applies only to this script process. It does not change the machine's permanent policy. Expected: `PASS: 16687 core checks`, then `Original value verifies: true` and `Changed value verifies: false`.

## Run the complete project

With an internet connection for Maven dependencies:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

First use downloads Maven and dependencies. The app binds to **127.0.0.1:8080**, local computer only. This HTTP mode is for the classroom demo. Press Ctrl+C to stop it.

Open `http://127.0.0.1:8080/hash?data=abc` to see the existing checksum feature. Open a second PowerShell terminal and run:

```powershell
$body = @{ values = @('Invoice A:100','Invoice B:200','Invoice C:300'); leafIndex = 1 } | ConvertTo-Json
$result = Invoke-RestMethod http://127.0.0.1:8080/merkle/proof -Method Post -ContentType 'application/json' -Body $body
$result | ConvertTo-Json -Depth 6
$trustedRoot = $result.root
$check = @{ value = 'Invoice B:200'; proof = $result.proof; expectedRoot = $trustedRoot }
Invoke-RestMethod http://127.0.0.1:8080/merkle/verify -Method Post -ContentType 'application/json' -Body ($check | ConvertTo-Json -Depth 6)
$check.value = 'Invoice B:999'
Invoke-RestMethod http://127.0.0.1:8080/merkle/verify -Method Post -ContentType 'application/json' -Body ($check | ConvertTo-Json -Depth 6)
```

The first verification should return `valid: true`, the changed record `valid: false`. Saving the root from this locally built sample demonstrates the flow. In a real system the verifier must obtain an authentic root independently of an untrusted proof provider.

## Files to review

- `src/main/java/com/snhu/merkle/MerkleTree.java`: nodes, recursive construction/traversal, proof verification, validation.
- `src/main/java/com/snhu/merkle/MerkleDemo.java`: sample records and operation counts.
- `src/main/java/com/snhu/controller/MerkleController.java`: root/proof/verify endpoints.
- `src/test/java/com/snhu/merkle/MerkleCoreChecks.java`: executable algorithm regression checks.
- `src/test/java/com/snhu/sslserver/MerkleApiTest.java`: HTTP-level checks to run through Maven.
- `ALGORITHM.md`: rules, complexity, tradeoffs, and references.
- `TEST_RESULTS.txt`: test results and remaining checks.

## API

`POST /merkle/root`: `{"values":["A","B","C"]}` returns root, leafCount, and buildHashCount.

`POST /merkle/proof`: `{"values":["A","B","C"],"leafIndex":1}` returns root and proof. Indexes start at zero. A proof has leafIndex, leafCount, and sibling hashes in leaf-to-root order.

`POST /merkle/verify`: `{"value":"B","expectedRoot":"64 hexadecimal characters","proof":{"leafIndex":1,"leafCount":3,"siblings":["...","..."]}}` returns a validity Boolean. Missing required fields/malformed JSON yields HTTP 400; a well-formed but invalid proof yields false.

The API is stateless. The proof endpoint rebuilds from supplied values each time (linear total work); only `proof(index)` on an already built tree and verification are logarithmic. No database, user authentication, or durable tree storage is provided.

## HTTPS configuration

The enhanced copy removes the old private keystores and hard-coded password. Private keystores are also omitted from the public original copy. Generate a new development keystore outside the project using your JDK:

```powershell
keytool -genkeypair -alias artemis -keyalg RSA -keysize 3072 -storetype PKCS12 -keystore "$env:USERPROFILE\artemis-dev.p12" -validity 365 -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
$env:ARTEMIS_KEYSTORE = "file:" + ($env:USERPROFILE -replace '\\','/') + '/artemis-dev.p12'
$secret = Read-Host 'Keystore password' -AsSecureString
$env:ARTEMIS_KEYSTORE_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=tls'
```

Use `https://localhost:8443/hash?data=abc`. A self-signed development certificate is not automatically trusted by your browser. HTTPS startup and certificate trust still need local verification. Remove the temporary password variable after stopping: `Remove-Item Env:ARTEMIS_KEYSTORE_PASSWORD`. Do not commit the keystore or password.

## Dependency audit and limits

```powershell
.\mvnw.cmd -Psecurity-audit verify
```

The explicit audit profile uses Dependency-Check 13.0.0 with a CVSS threshold of 7. An optional NVD API key is read from `NVD_API_KEY`, not stored in the POM. The scan needs internet access and may take time; it has not been run here. The baseline's old suppression file is not carried forward.

Spring Boot is pinned to 3.5.16 as an intermediate update from the old 2.2.4 baseline. Its open-source support has ended; this is a local coursework build, not a production deployment. Moving to a supported 4.x generation and rerunning compatibility/security tests remains release work. No claim of vulnerability-free dependencies is made.

Input limits: at most 4096 leaves, 4096 UTF-8 bytes per value, 1 MiB total record data. Empty strings are allowed, nulls and malformed Unicode are rejected. Bounds apply after JSON deserialization; an exposed service would also need transport body-size limits, authentication, and rate limiting. Hashes prove consistency with a trusted root, not identity, authorization, truth of a record, or confidentiality. Repeated equal values can have proofs at multiple valid positions.

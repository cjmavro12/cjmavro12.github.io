# CS 305 Project Two — Artemis Financial (Refactor on Official Code Base)

## Rubric Checklist
- [x] SHA-256 checksum endpoint at **`/hash`**
- [x] HTTPS enabled on **`8443`** using **`keystore.p12`**
- [x] OWASP Dependency-Check Maven plugin
- [x] Keytool commands + CER export instructions
- [x] Suppressions template for vetted false positives
- [x] Screenshots list for submission

## Commands
```
mvn spring-boot:run
# https://localhost:8443/hash?data=CJ%20Mavrogeorge-ProjectTwo-YYYY-MM-DD
mvn -U org.owasp:dependency-check-maven:check
mvn clean package
java -jar target/*.jar
```

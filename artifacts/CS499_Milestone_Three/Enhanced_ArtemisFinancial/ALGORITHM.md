# Algorithm and complexity

## Deterministic rules

This is a custom classroom SHA-256 Merkle scheme, not a Certificate Transparency protocol implementation. It uses different prefixes for leaves and internal nodes, a practice described in RFC 9162 section 2.1.1. The odd-node rule and count commitment below are specific to this project.

- Values are ordered, exact UTF-8 strings. No trimming or Unicode normalization changes the records.
- Leaf: SHA256(0x00 || UTF8(value)).
- Internal node: SHA256(0x01 || leftHash || rightHash), joining raw 32-byte hashes.
- At every odd-width level, duplicate the final hash to create its parent. Node references are shared rather than copying the subtree.
- Returned root: SHA256(0x02 || countAsFourBigEndianBytes || internalTreeRoot).
- Empty collection: SHA256(0x02 || four zero bytes), with no internal hash and no valid membership proof.
- A one-value collection has one leaf, a root commitment, and an empty sibling list.

The count commitment matters: without it, duplicating a final value can make `[A,B,C]` and `[A,B,C,C]` share the same tree hash. Including the count distinguishes the collections. Hash domain prefixes distinguish records from combined child hashes. Proof verification checks both metadata and hashes.

## Construction and proof algorithms

1. Validate limits and hash each record into an immutable leaf node.
2. `buildLevel` pairs neighboring nodes, creates parents, and recursively calls itself on the smaller parent list until one node remains.
3. Each node stores its hash, two children, and the original leaf-index range. `collect` follows the target index through one child at each level and adds the sibling hash as recursion returns.
4. `verify` hashes the supplied record, derives left/right position from index parity, and combines sibling hashes until it reaches the internal root. At an odd duplicated position it also checks that the sibling equals the current hash.
5. Verification requires exactly the expected path length, valid index/count, and 32-byte hexadecimal hashes. It wraps the result with the count commitment and compares it to the separately supplied trusted root.

## Cost model

Let n be the number of records, B their total UTF-8 byte length, and L the selected record's byte length. SHA-256 must read all bytes, so variable-length inputs must be included in the analysis.

| Operation | Time | Additional/storage cost |
| --- | --- | --- |
| Build complete tree | O(B + n) | O(n) nodes/hashes plus transient per-value encoding |
| Read cached root | O(1) | O(1) |
| Produce proof from cached tree | O(log n) for n > 1 | O(log n) sibling hashes and recursion |
| Verify one value | O(L + log n) | O(1) working hash state beyond the supplied O(log n) proof |
| API request that builds and returns proof | O(B + n) | O(n), including the rebuilt tree |
| Rebuild after a changed record | O(B + n) | O(n); incremental updates are not implemented |

Construction recursion depth is O(log n). Validation and leaf encoding add work proportional to input bytes. Stored hashes are fixed at 32 bytes. The input strings are supplied by the caller and are not retained by the tree.

For n=2^k: n leaf hashes + (n-1) internal hashes + one count commitment = 2n build hashes. A valid proof contains k siblings and uses k+2 hashes (leaf + siblings + commitment). The demo records exact hash-operation counts, not wall-clock speed. SHA-256 calls on records of different byte lengths are not equal-cost timing measurements.

The tree uses more memory than a single checksum but permits verification of one record without sending the full collection after a root and proof exist. It preserves ordering; this is not a sorted tree, a signature, or encryption. A trusted root is essential because an attacker able to replace both data and the trusted root can create a self-consistent replacement tree.

## Sources

Laurie, B., Messeri, E., & Stradling, R. (2021). Certificate Transparency Version 2.0 (RFC 9162), section 2.1.1. https://www.rfc-editor.org/rfc/rfc9162.html

Oracle. (n.d.). MessageDigest. https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html

Spring. (2026, June 25). Spring Boot 3.5.16 available now. https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/

OWASP Dependency-Check. (n.d.). dependency-check:check. https://dependency-check.github.io/DependencyCheck/dependency-check-maven/check-mojo.html

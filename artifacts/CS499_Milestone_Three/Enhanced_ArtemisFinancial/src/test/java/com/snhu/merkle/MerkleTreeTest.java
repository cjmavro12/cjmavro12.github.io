package com.snhu.merkle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
class MerkleTreeTest {
 @Test void completeAlgorithmRegressionSuite() { assertTrue(MerkleCoreChecks.runAll() > 16000); }
}

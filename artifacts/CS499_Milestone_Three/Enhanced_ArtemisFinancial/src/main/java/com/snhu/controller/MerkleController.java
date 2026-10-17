package com.snhu.controller;
import com.snhu.merkle.MerkleTree;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merkle")
public class MerkleController {
    public record BuildRequest(List<String> values) {}
    public record ProofRequest(List<String> values, Integer leafIndex) {}
    public record ProofData(Integer leafIndex, Integer leafCount, List<String> siblings) {}
    public record VerifyRequest(String value, ProofData proof, String expectedRoot) {}
    public record TreeResponse(String root, int leafCount, int buildHashCount) {}
    public record ProofResponse(String root, MerkleTree.Proof proof) {}
    @PostMapping("/root")
    public TreeResponse root(@RequestBody BuildRequest request) {
        if (request == null) throw new IllegalArgumentException("Missing request");
        MerkleTree tree = new MerkleTree(request.values());
        return new TreeResponse(tree.getRootHash(), tree.getLeafCount(), tree.getBuildHashCount());
    }
    @PostMapping("/proof")
    public ProofResponse proof(@RequestBody ProofRequest request) {
        if (request == null || request.leafIndex() == null) throw new IllegalArgumentException("Missing leaf index");
        MerkleTree tree = new MerkleTree(request.values());
        return new ProofResponse(tree.getRootHash(), tree.proof(request.leafIndex()));
    }
    @PostMapping("/verify")
    public Map<String,Boolean> verify(@RequestBody VerifyRequest request) {
        if (request == null || request.value() == null || request.expectedRoot() == null
                || request.proof() == null || request.proof().leafIndex() == null
                || request.proof().leafCount() == null || request.proof().siblings() == null)
            throw new IllegalArgumentException("Supply value, expectedRoot, and complete proof");
        ProofData p = request.proof();
        return Map.of("valid", MerkleTree.verify(request.value(),
                new MerkleTree.Proof(p.leafIndex(), p.leafCount(), p.siblings()), request.expectedRoot()));
    }
}

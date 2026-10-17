package com.snhu.merkle;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
public final class MerkleDemo {
    public static void main(String[] args) {
        List<String> records = Arrays.asList("Invoice A:100", "Invoice B:200", "Invoice C:300");
        MerkleTree tree = new MerkleTree(records);
        MerkleTree.Proof proof = tree.proof(1);
        System.out.println("Root: " + tree.getRootHash());
        System.out.println("Leaf count: " + tree.getLeafCount());
        System.out.println("Proof siblings: " + proof.getSiblings());
        System.out.println("Original value verifies: " + MerkleTree.verify(records.get(1), proof, tree.getRootHash()));
        System.out.println("Changed value verifies: " + MerkleTree.verify("Invoice B:999", proof, tree.getRootHash()));
        System.out.println("n,build_sha256_calls,proof_siblings,verify_sha256_calls");
        for (int n : new int[]{1,2,3,8,64,256,1024,4096}) {
            List<String> values = new ArrayList<>();
            for (int i=0;i<n;i++) values.add("Record " + i);
            MerkleTree t = new MerkleTree(values);
            int depth = t.proof(n-1).getSiblings().size();
            System.out.println(n + "," + t.getBuildHashCount() + "," + depth + "," + (depth+2));
        }
    }
}

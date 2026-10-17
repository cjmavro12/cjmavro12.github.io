package com.snhu.merkle;
import java.util.*;
/** Runs without JUnit or Maven; the JUnit adapter runs these same checks. */
public final class MerkleCoreChecks {
    private static int checks;
    private static void check(boolean condition, String name) {
        checks++; if (!condition) throw new AssertionError(name);
    }
    private static void rejects(Runnable action, String name) {
        try { action.run(); throw new AssertionError("Expected rejection: " + name); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    public static void main(String[] args) { System.out.println("PASS: " + runAll() + " core checks"); }
    public static int runAll() {
        checks=0;
        check(MerkleTree.checksum("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"), "known SHA-256");
        MerkleTree empty = new MerkleTree(List.of());
        check(empty.getRootHash().equals("395c2f5598a1643a205154c6f4c46ce36895b28e6c35660a95e5c6fd5ef9aeab"), "independent empty vector");
        rejects(() -> empty.proof(0), "empty has no proof");
        check(new MerkleTree(List.of("a")).getRootHash().equals("1f3e6f0027812288af3eb7aa1df97bf603a76b2c81eb3391cef85b9b26521c5a"), "independent single vector");
        check(new MerkleTree(List.of("a","b","c")).getRootHash().equals("b1875634a4e55d8543d546ac521c8a1a8fc91a729297f6dcf40b6be1b8a31b7b"), "independent odd vector");
        check(new MerkleTree(List.of("a","b","c","d")).getRootHash().equals("521a147ce94cdf44b171f4160d8b0f3b2d0ffee9f7487831ca98fa1c81fe09ae"), "independent even vector");
        check(new MerkleTree(List.of("猫","😀", "")).getRootHash().equals("174715a74f0476ea4701865989c9b672f6a6b1d7c45abc1441e2a98265a46263"), "independent Unicode vector");
        check(!new MerkleTree(List.of("a","b","c")).getRootHash().equals(new MerkleTree(List.of("a","b","c","c")).getRootHash()), "count commitment separates duplicate-last ambiguity");
        Random random = new Random(499);
        for (int n=1;n<=128;n++) {
            List<String> values = new ArrayList<>();
            for(int i=0;i<n;i++) values.add("record-"+i+"-"+random.nextInt());
            MerkleTree tree = new MerkleTree(values);
            check(tree.getRootHash().equals(new MerkleTree(values).getRootHash()), "deterministic root");
            for(int i=0;i<n;i++) {
                MerkleTree.Proof proof = tree.proof(i);
                check(MerkleTree.verify(values.get(i),proof,tree.getRootHash()), "membership n="+n+" index="+i);
                check(!MerkleTree.verify(values.get(i)+"changed",proof,tree.getRootHash()), "tampered data");
            }
        }
        MerkleTree tree = new MerkleTree(List.of("A","B","C","D","E"));
        MerkleTree.Proof proof = tree.proof(4);
        for(int i=0;i<proof.getSiblings().size();i++) {
            List<String> siblings = new ArrayList<>(proof.getSiblings()); siblings.set(i,"0".repeat(64));
            check(!MerkleTree.verify("E",new MerkleTree.Proof(4,5,siblings),tree.getRootHash()), "tampered sibling");
        }
        check(!MerkleTree.verify("E",proof,"0".repeat(64)), "wrong root");
        check(!MerkleTree.verify("E",proof,"not hex"), "malformed root");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(3,5,proof.getSiblings()),tree.getRootHash()), "wrong index");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(4,6,proof.getSiblings()),tree.getRootHash()), "wrong count");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(4,5,List.of()),tree.getRootHash()), "short proof");
        List<String> longer=new ArrayList<>(proof.getSiblings()); longer.add("0".repeat(64));
        check(!MerkleTree.verify("E",new MerkleTree.Proof(4,5,longer),tree.getRootHash()), "long proof");
        List<String> invalid=new ArrayList<>(proof.getSiblings()); invalid.set(0,"zz");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(4,5,invalid),tree.getRootHash()), "invalid hex sibling");
        invalid.set(0,null);
        check(!MerkleTree.verify("E",new MerkleTree.Proof(4,5,invalid),tree.getRootHash()), "null sibling");
        check(!MerkleTree.verify("E",null,tree.getRootHash()), "null proof");
        check(!MerkleTree.verify(null,proof,tree.getRootHash()), "null value");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(-1,5,List.of()),tree.getRootHash()), "negative index");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(5,5,List.of()),tree.getRootHash()), "outside index");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(0,0,List.of()),tree.getRootHash()), "zero count");
        check(!MerkleTree.verify("E",new MerkleTree.Proof(0,4097,List.of()),tree.getRootHash()), "oversized count");
        rejects(() -> tree.proof(-1), "negative creation index");
        rejects(() -> tree.proof(5), "outside creation index");
        rejects(() -> new MerkleTree(null), "null list");
        rejects(() -> new MerkleTree(Arrays.asList("a",null)), "null record");
        rejects(() -> new MerkleTree(Collections.nCopies(4097,"a")), "too many records");
        rejects(() -> new MerkleTree(List.of("a".repeat(4097))), "oversized record");
        rejects(() -> new MerkleTree(List.of("猫".repeat(2000))), "UTF-8 byte limit");
        rejects(() -> new MerkleTree(Collections.nCopies(257,"a".repeat(4096))), "total byte limit");
        rejects(() -> new MerkleTree(List.of("\uD800")), "unpaired surrogate");
        MerkleTree dup = new MerkleTree(List.of("same","same","same"));
        for(int i=0;i<3;i++) check(MerkleTree.verify("same",dup.proof(i),dup.getRootHash()), "duplicate values indexed");
        MerkleTree text = new MerkleTree(List.of("", "猫", "😀"));
        for(int i=0;i<3;i++) check(MerkleTree.verify(List.of("","猫","😀").get(i),text.proof(i),text.getRootHash()), "UTF-8 proof");
        check(!new MerkleTree(List.of("a","b")).getRootHash().equals(new MerkleTree(List.of("b","a")).getRootHash()), "order matters");
        MerkleTree max = new MerkleTree(Collections.nCopies(4096,"x"));
        check(max.getBuildHashCount()==8192, "4096 build hash operations");
        check(max.proof(4095).getSiblings().size()==12, "4096 proof depth");
        check(MerkleTree.verify("x",max.proof(4095),max.getRootHash()), "max tree proof");
        List<String> original = new ArrayList<>(List.of("a","b"));
        MerkleTree immutable = new MerkleTree(original); String saved = immutable.getRootHash(); original.set(0,"changed");
        check(saved.equals(immutable.getRootHash()), "tree independent of caller list");
        List<String> source = new ArrayList<>(proof.getSiblings());
        MerkleTree.Proof copied = new MerkleTree.Proof(4,5,source); source.clear();
        check(MerkleTree.verify("E",copied,tree.getRootHash()), "proof defensive copy");
        try { copied.getSiblings().clear(); throw new AssertionError("mutable proof"); }
        catch(UnsupportedOperationException expected) { checks++; }
        return checks;
    }
}

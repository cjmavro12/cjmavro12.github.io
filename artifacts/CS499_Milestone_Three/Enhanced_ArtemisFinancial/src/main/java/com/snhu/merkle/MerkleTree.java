package com.snhu.merkle;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable, ordered SHA-256 tree. See ALGORITHM.md for the custom wire format. */
public final class MerkleTree {
    public static final int MAX_LEAVES = 4096;
    public static final int MAX_VALUE_BYTES = 4096;
    public static final int MAX_TOTAL_BYTES = 1_048_576;

    // Duplicated right children reuse the same immutable node; no copied subtree.
    private static final class Node {
        final byte[] hash;
        final Node left, right;
        final int first, last;
        Node(byte[] hash, Node left, Node right, int first, int last) {
            this.hash = hash; this.left = left; this.right = right;
            this.first = first; this.last = last;
        }
    }
    public static final class Proof {
        private final int leafIndex, leafCount;
        private final List<String> siblings;
        public Proof(int leafIndex, int leafCount, List<String> siblings) {
            if (siblings == null) throw new IllegalArgumentException("Missing siblings");
            this.leafIndex = leafIndex; this.leafCount = leafCount;
            this.siblings = Collections.unmodifiableList(new ArrayList<>(siblings));
        }
        public int getLeafIndex() { return leafIndex; }
        public int getLeafCount() { return leafCount; }
        public List<String> getSiblings() { return siblings; }
    }
    private final Node root;
    private final int size;
    private final String rootHash;
    private int buildHashCount;

    public MerkleTree(List<String> values) {
        if (values == null || values.size() > MAX_LEAVES)
            throw new IllegalArgumentException("Supply 0 to 4096 values");
        size = values.size();
        List<Node> leaves = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < size; i++) {
            byte[] value = valueBytes(values.get(i));
            total += value.length;
            if (total > MAX_TOTAL_BYTES) throw new IllegalArgumentException("Total input exceeds 1 MiB");
            leaves.add(new Node(digest((byte) 0, value), null, null, i, i));
            buildHashCount++;
        }
        root = leaves.isEmpty() ? null : buildLevel(leaves);
        rootHash = hex(commit(size, root == null ? new byte[0] : root.hash));
        buildHashCount++;
    }
    // Pair neighbors, duplicate the final hash at odd levels, recurse to one root.
    private Node buildLevel(List<Node> level) {
        if (level.size() == 1) return level.get(0);
        List<Node> parents = new ArrayList<>();
        for (int i = 0; i < level.size(); i += 2) {
            Node left = level.get(i);
            Node right = i + 1 < level.size() ? level.get(i + 1) : left;
            parents.add(new Node(digest((byte) 1, left.hash, right.hash),
                    left, right, left.first, right.last));
            buildHashCount++;
        }
        return buildLevel(parents);
    }
    public String getRootHash() { return rootHash; }
    public int getLeafCount() { return size; }
    public int getBuildHashCount() { return buildHashCount; }
    public Proof proof(int index) {
        if (index < 0 || index >= size) throw new IllegalArgumentException("Leaf index is out of range");
        List<String> path = new ArrayList<>();
        collect(root, index, path);
        return new Proof(index, size, path);
    }
    // Only visit the target branch. Append siblings on return: leaf to root order.
    private static void collect(Node node, int index, List<String> path) {
        if (node.left == null) return;
        if (index <= node.left.last) {
            collect(node.left, index, path); path.add(hex(node.right.hash));
        } else {
            collect(node.right, index, path); path.add(hex(node.left.hash));
        }
    }
    /** expectedRoot must come from a trusted source, separately from the proof. */
    public static boolean verify(String value, Proof proof, String expectedRoot) {
        if (proof == null || proof.leafCount < 1 || proof.leafCount > MAX_LEAVES
                || proof.leafIndex < 0 || proof.leafIndex >= proof.leafCount) return false;
        try {
            int width = proof.leafCount, index = proof.leafIndex, step = 0;
            int depth = 0;
            for (int w = width; w > 1; w = (w + 1) / 2) depth++;
            if (proof.siblings.size() != depth) return false;
            byte[] expected = unhex(expectedRoot);
            byte[] current = digest((byte) 0, valueBytes(value));
            while (width > 1) {
                byte[] sibling = unhex(proof.siblings.get(step++));
                if ((index & 1) == 1) current = digest((byte) 1, sibling, current);
                else {
                    // The duplicated sibling must equal the current hash.
                    if (index + 1 >= width && !MessageDigest.isEqual(current, sibling)) return false;
                    current = digest((byte) 1, current, sibling);
                }
                index /= 2; width = (width + 1) / 2;
            }
            return MessageDigest.isEqual(expected, commit(proof.leafCount, current));
        } catch (IllegalArgumentException e) { return false; }
    }
    public static String checksum(String value) {
        return hex(sha256(valueBytes(value)));
    }
    private static byte[] valueBytes(String value) {
        if (value == null || value.length() > MAX_VALUE_BYTES)
            throw new IllegalArgumentException("A value must contain at most 4096 UTF-8 bytes");
        // Reject malformed UTF-16 instead of silently replacing it during encoding.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i)))
                    throw new IllegalArgumentException("Malformed Unicode value");
            } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Malformed Unicode value");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_VALUE_BYTES) throw new IllegalArgumentException("Value exceeds 4096 UTF-8 bytes");
        return bytes;
    }
    private static byte[] commit(int count, byte[] treeHash) {
        return digest((byte) 2, ByteBuffer.allocate(4).putInt(count).array(), treeHash);
    }
    private static byte[] digest(byte prefix, byte[]... parts) {
        MessageDigest md = newDigest(); md.update(prefix);
        for (byte[] part : parts) md.update(part);
        return md.digest();
    }
    private static MessageDigest newDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
    private static byte[] sha256(byte[] bytes) { return newDigest().digest(bytes); }
    private static String hex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            chars[2 * i] = alphabet[(bytes[i] & 255) >>> 4]; chars[2 * i + 1] = alphabet[bytes[i] & 15];
        }
        return new String(chars);
    }
    private static byte[] unhex(String text) {
        if (text == null || !text.matches("[0-9a-fA-F]{64}"))
            throw new IllegalArgumentException("Expected 64 hexadecimal characters");
        byte[] bytes = new byte[32];
        for (int i = 0; i < 32; i++) bytes[i] = (byte) Integer.parseInt(text.substring(2*i,2*i+2),16);
        return bytes;
    }
}

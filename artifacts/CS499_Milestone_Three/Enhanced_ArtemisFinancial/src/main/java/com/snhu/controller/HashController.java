package com.snhu.controller;
import com.snhu.merkle.MerkleTree;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class HashController {
    // Preserves the original checksum behavior, with an input bound.
    @GetMapping(value="/hash", produces=MediaType.TEXT_PLAIN_VALUE)
    public String hash(@RequestParam(name="data") String data) {
        return MerkleTree.checksum(data);
    }
}

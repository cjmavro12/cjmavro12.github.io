package com.snhu.sslserver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MerkleApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Test void originalChecksumIsPreserved() throws Exception {
        mvc.perform(get("/hash").param("data","abc")).andExpect(status().isOk())
            .andExpect(content().string("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
    }
    @Test void proofRoundTripAndTampering() throws Exception {
        String body=mvc.perform(post("/merkle/proof").contentType("application/json")
            .content("{\"values\":[\"A\",\"B\",\"C\"],\"leafIndex\":1}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode response=json.readTree(body);
        var request=json.createObjectNode().put("value","B").put("expectedRoot",response.get("root").asText());
        request.set("proof",response.get("proof"));
        mvc.perform(post("/merkle/verify").contentType("application/json").content(request.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
        request.put("value","changed");
        mvc.perform(post("/merkle/verify").contentType("application/json").content(request.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false));
    }
    @Test void emptyRootButNoEmptyProof() throws Exception {
        mvc.perform(post("/merkle/root").contentType("application/json").content("{\"values\":[]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.leafCount").value(0));
        mvc.perform(post("/merkle/proof").contentType("application/json").content("{\"values\":[],\"leafIndex\":0}"))
            .andExpect(status().isBadRequest());
    }
    @Test void missingAndMalformedInputsAreRejected() throws Exception {
        for(String body : new String[]{"{}","{\"values\":[null]}","{broken"})
            mvc.perform(post("/merkle/root").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/merkle/verify").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
}

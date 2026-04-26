package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Sha256HexTest {

    @Test
    void emptyInputReturnsKnownHash() {
        // sha256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Sha256Hex.hexOf(new byte[0]));
    }

    @Test
    void abcReturnsKnownHash() {
        // sha256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Sha256Hex.hexOf("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void outputIsAlwaysLowercaseAnd64Chars() {
        String h = Sha256Hex.hexOf("anything".getBytes(StandardCharsets.UTF_8));
        assertEquals(64, h.length());
        assertEquals(h.toLowerCase(), h);
    }

    @Test
    void nullInputThrows() {
        assertThrows(NullPointerException.class, () -> Sha256Hex.hexOf(null));
    }
}

package com.nanolaba.nrg.widgets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Sha256Hex {

    private Sha256Hex() {
    }

    static String hexOf(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}

package com.nanolaba.nrg.widgets;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Computes the lowercase-hex SHA-256 of a byte array.
 *
 * <p>Used by the remote-import path to verify {@code sha256='…'} pins and to log the
 * actual hash of newly fetched payloads so authors can paste the value back into the
 * source for reproducible downloads.
 */
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
        return String.format(Locale.ROOT, "%064x", new BigInteger(1, md.digest(data)));
    }
}

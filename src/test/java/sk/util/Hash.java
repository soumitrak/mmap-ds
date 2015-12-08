package sk.util;

import sk.mmap.Constants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
    public static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MessageDigest getDigest() {
        return getDigest("SHA-256");
    }

    private static String byteArrayToHex(byte[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            // builder.append(Integer.toString((array[i] & 0xff) + 0x100, 16).substring(1));
            builder.append(Integer.toString(array[i], 16));
        }
        return builder.toString();
    }

    public static String hash(MessageDigest digest) {
        return byteArrayToHex(digest.digest());
    }

    public static MessageDigest update(MessageDigest digest, byte input) {
        digest.update(input);
        return digest;
    }

    public static MessageDigest update(MessageDigest digest, byte[] input) {
        digest.update(input);
        return digest;
    }

    public static MessageDigest update(MessageDigest digest, int input) {
        for (int i = 0; i < Constants.SIZE_OF_INT; i++) {
            digest.update((byte) (input >> i));
        }

        return digest;
    }

    public static MessageDigest update(MessageDigest digest, long input) {
        for (int i = 0; i < Constants.SIZE_OF_LONG; i++) {
            digest.update((byte) (input >> i));
        }

        return digest;
    }

    public static MessageDigest update(MessageDigest digest, float input) {
        return update(digest, Float.floatToRawIntBits(input));
    }

    public static MessageDigest update(MessageDigest digest, double input) {
        return update(digest, Double.doubleToRawLongBits(input));
    }
}

package com.example.demo.config;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

@Component
public class FernetUtil {
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final byte VERSION = (byte) 0x80; // 128
    private static final int IV_LENGTH = 16;
    private static final byte[] SECRET = "hello-this-is-multi-role-based-login-system".getBytes();
    private static final byte[] AES_KEY = Arrays.copyOfRange(SECRET, 0, 16);
    private static final byte[] HMAC_KEY = Arrays.copyOfRange(SECRET, 16, 32);
    

    public static String encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        long timestamp = Instant.now().getEpochSecond();
        byte[] timestampBytes = longToBytes(timestamp);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES_KEY, "AES"), new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        byte[] header = concat(new byte[]{VERSION}, timestampBytes, iv, ciphertext);
        byte[] hmac = hmacSha256(header);
        byte[] finalToken = concat(header, hmac);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(finalToken);
    }

    public static String decrypt(String token) throws Exception {
        byte[] fullData = Base64.getUrlDecoder().decode(token);
        if (fullData.length < 1 + 8 + 16 + 32) {
            throw new IllegalArgumentException("Invalid token size");
        }

        byte version = fullData[0];
        if (version != VERSION) throw new SecurityException("Invalid version byte");

        byte[] timestamp = Arrays.copyOfRange(fullData, 1, 9);
        byte[] iv = Arrays.copyOfRange(fullData, 9, 25);
        byte[] hmac = Arrays.copyOfRange(fullData, fullData.length - 32, fullData.length);
        byte[] ciphertext = Arrays.copyOfRange(fullData, 25, fullData.length - 32);

        byte[] messageWithoutHmac = Arrays.copyOfRange(fullData, 0, fullData.length - 32);
        byte[] expectedHmac = hmacSha256(messageWithoutHmac);
        if (!Arrays.equals(hmac, expectedHmac)) {
            throw new SecurityException("HMAC mismatch – tampered or invalid token");
        }

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(AES_KEY, "AES"), new IvParameterSpec(iv));
        byte[] plainBytes = cipher.doFinal(ciphertext);
        return new String(plainBytes, "UTF-8");
    }

    // HMAC-SHA256
    private static byte[] hmacSha256(byte[] data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(HMAC_KEY, HMAC_ALGO));
        return mac.doFinal(data);
    }

    // Convert long to 8-byte array
    private static byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    // Concatenate multiple byte arrays
    private static byte[] concat(byte[]... arrays) {
        int length = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
}

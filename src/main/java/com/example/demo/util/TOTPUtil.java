package com.example.demo.util;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;

public class TOTPUtil {

    private static final String HMAC_ALGO = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int OTP_DIGITS = 6;

    public static String generateTOTP(String base32Secret) {
        long timeWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        return generateTOTPForTime(base32Secret, timeWindow);
    }

    public static boolean verifyTOTP(String base32Secret, String code) {
        long currentWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        // Check current window, previous, and next (to allow some time drift)
        for (int i = -1; i <= 1; i++) {
            String expected = generateTOTPForTime(base32Secret, currentWindow + i);
            if (expected.equals(code)) return true;
        }
        return false;
    }

    private static String generateTOTPForTime(String base32Secret, long timeWindow) {
        try {
            byte[] key = new Base32().decode(base32Secret.toUpperCase());
            byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();

            SecretKeySpec signKey = new SecretKeySpec(key, HMAC_ALGO);
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary =
                    ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % (int) Math.pow(10, OTP_DIGITS);
            return String.format("%0" + OTP_DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to generate TOTP", e);
        }
    }
}

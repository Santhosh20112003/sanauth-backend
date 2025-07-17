package com.example.demo.service;

import com.example.demo.util.TOTPUtil;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
public class TOTPService {

    @Autowired
    private RedisService redisService;

    private static final String SECRET_KEY_PREFIX = "totp:secret:";
    private static final String USED_OTP_KEY_PREFIX = "totp:otp:";
    
    @Value("${application.issuer.name}")
    private String ISSUER; 

    public Map<String, String> register(String username) {
        String redisKey = SECRET_KEY_PREFIX + username;

        // Check if user already has a secret
        String existingSecret = (String) redisService.get(redisKey);
        if (existingSecret != null) {
            // Optionally avoid sending QR again
            Map<String, String> result = new HashMap<>();
            result.put("message", "TOTP already registered for user");
            result.put("secret", existingSecret);
            result.put("qrUrl", "QR already generated"); // Or omit this
            result.put("otpAuthUrl", "Already registered"); // Or return existing one
            return result;
        }

        // Create new secret
        String secret = generateBase32Secret();
        redisService.save(redisKey, secret);

        // Generate OTP URL
        String otpAuthUrl = "otpauth://totp/" + ISSUER + ":" + username
                + "?secret=" + secret
                + "&issuer=" + ISSUER;

        String encodedOtpAuthUrl = URLEncoder.encode(otpAuthUrl, StandardCharsets.UTF_8);
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?data=" + encodedOtpAuthUrl + "&size=400x400";

        Map<String, String> result = new HashMap<>();
        result.put("secret", secret);
        result.put("qrUrl", qrImageUrl);
        result.put("otpAuthUrl", otpAuthUrl);
        result.put("message", "TOTP registration successful");
        return result;
    }

    public boolean verifyCode(String username, String code) {
        if (code == null || code.length() != 6 || !code.matches("\\d+")) {
            return false; // Basic format check
        }

        String secret = (String) redisService.get(SECRET_KEY_PREFIX + username);
        if (secret == null) {
            return false; // User not registered
        }

        String usedOtpKey = USED_OTP_KEY_PREFIX + code;
        Object lastUsed = redisService.get(usedOtpKey);
        if (lastUsed != null && lastUsed.toString().equals(code)) {
            return false; // OTP reuse
        }

        boolean isValid = TOTPUtil.verifyTOTP(secret, code);
        if (isValid) {
            redisService.save(usedOtpKey, code);
        }
        return isValid;
    }

    private String generateBase32Secret() {
        byte[] buffer = new byte[10];
        new SecureRandom().nextBytes(buffer);
        return new Base32().encodeToString(buffer).replace("=", "");
    }
}

package com.example.demo.config;

import java.util.Date;
import java.util.LinkedHashMap;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.demo.modal.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
	
	@Autowired
	private FernetUtil fernetUtil;

    private static final String SECRET_KEY_STRING = "e970d4a6ae7042fc886ee6744e01070de500e3242205006e080bc537a237119b";
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    public String generateToken(String email, String role) {
        String token =  Jwts.builder()
                .setSubject("User Details")
                .setHeaderParam("typ", "JWT")
                .claim("data", new Payload(email, role))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+ 30 * 60 * 1000)) 
                .signWith(SECRET_KEY, Jwts.SIG.HS256)
                .compact();
        
        try {
			return fernetUtil.encrypt(token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
    	System.out.println("Validating token.. ");
    	
        Payload payload = extractPayload(token);
        try {
			token = fernetUtil.decrypt(token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (payload == null) return false;
        
        String tokenEmail = payload.getEmail();
        String tokenRole = payload.getRole();

        String userEmail = userDetails.getUsername();
        String userRoles = userDetails.getAuthorities().toString();

        return tokenEmail.equals(userEmail) && userRoles.contains(tokenRole);
    }
    
    public Boolean isTokenExpired(String token) {
		try {
			try {
				token = fernetUtil.decrypt(token);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Claims claims = Jwts.parser()
					.verifyWith(SECRET_KEY)
					.build()
					.parseSignedClaims(token)
					.getBody();
			return claims.getExpiration().before(new Date());
		} catch (Exception e) {
			System.err.println("Error checking token expiration: " + e.getMessage());
			return true; 
		}
	}

    public Payload extractPayload(String token) {
        try {
        	
    			token = fernetUtil.decrypt(token);
    		
        	Payload payload = new Payload();
            Claims claims = Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getBody();
            
            LinkedHashMap<String, Object> data = claims.get("data", LinkedHashMap.class);
            payload.setEmail((String) data.get("email"));
            payload.setRole((String) data.get("role"));
            return payload;
        } catch (Exception e) {
            System.err.println("Error extracting payload from token: " + e.getMessage());
            return null;
        }
    }
}

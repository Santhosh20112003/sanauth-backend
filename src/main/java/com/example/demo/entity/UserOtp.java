package com.example.demo.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_otp")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOtp {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	private String email;
	
	@Column(nullable = false,unique = true)
	private Integer otp;
	
	private String type;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
	
	private Boolean used = false;
	
	public static boolean isExpired(Instant createdAt, long expiryInSeconds) {
	    return createdAt.plusSeconds(expiryInSeconds).isBefore(Instant.now());
	}

}

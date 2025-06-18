package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserOtp;


public interface OtpRepository extends JpaRepository<UserOtp, Long> {
	Optional<UserOtp> findByEmail(String email);
	
	
	Optional<UserOtp> findTopByEmailAndTypeAndUsedOrderByCreatedAtDesc(String email, String type, boolean used);


	Optional<UserOtp> findByEmailAndType(String email, String type);
	
	
}

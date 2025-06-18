package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.User;
import com.example.demo.modal.RefinedUserModal;

import jakarta.transaction.Transactional;

public interface UserRepository extends JpaRepository<User, String> {
	Optional<User> findByEmail(String email);

//	@Transactional
//	@Modifying
//	@Query("UPDATE User u SET u.otp = :otp WHERE u.email = :email")
//	int updateOtpByEmail(@Param("email") String email, @Param("otp") Integer otp);

//	@Transactional
//	@Modifying
//	@Query("UPDATE User u SET u.forgot_password_otp = :otp WHERE u.email = :email")
//	int updateforgotOtpByEmail(@Param("email") String email, @Param("otp") Integer otp);

}

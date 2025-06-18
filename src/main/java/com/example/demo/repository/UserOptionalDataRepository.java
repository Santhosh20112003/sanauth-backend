package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserOptionalData;

public interface UserOptionalDataRepository extends JpaRepository<UserOptionalData, Long> {

	Optional<UserOptionalData> findByEmail(String email);
  
}

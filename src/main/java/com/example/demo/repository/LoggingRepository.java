package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserLoginHistory;


public interface LoggingRepository extends JpaRepository<UserLoginHistory,Long > {
	List<UserLoginHistory> findByEmail(String email);
}
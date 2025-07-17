package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

		Optional<UserSettings> findByEmail(String email);
}

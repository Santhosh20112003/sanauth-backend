package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_settings")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserSettings {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique = true,nullable = false)
	private String email;
	
	//default is false, if true then user will be able to login with email and password
	@Column(nullable = false)
	private boolean isMfaEnabled;
}

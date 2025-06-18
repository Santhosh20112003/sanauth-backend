package com.example.demo.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_optional_data")
public class UserOptionalData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="email", unique = true, nullable = false)
	private String email;

	// Date of Birth
	@Column(name = "dob")
	private LocalDate dob;

	// Gender (e.g., Male, Female, Other)
	@Column(name = "gender")
	private String gender;

	// Contact phone number
	@Column(name = "phone")
	private String phone;

	// User's location (city, state, etc.)
	@Column(name = "location")
	private String location;

	// Short biography or about section
	@Column(name = "bio", length = 1000)
	private String bio;

	// User's personal or professional website
	@Column(name = "website_url")
	private String websiteUrl;

	// Current job title (e.g., Software Developer)
	@Column(name = "job_title")
	private String jobTitle;

}

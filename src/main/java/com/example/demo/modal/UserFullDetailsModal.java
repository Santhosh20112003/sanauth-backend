package com.example.demo.modal;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFullDetailsModal {

	private String uid;
    private String email;
    private String name;
    private String photoURL;
    private String role;
    private Instant createdAt;
    private Instant lastLogin;
    private boolean isActive;
    private boolean isEmailVerified;
	private LocalDate dob;
	private String gender;
	private String phone;
	private String location;
	private String bio;
	private String websiteUrl;
	private String jobTitle;
	
}

package com.example.demo.modal;

import java.time.Instant;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDetailsModal {
    private String name;
    private String email;
    private String photoURL;
	private LocalDate dob;
	private String gender;
	private String phone;
	private String location;
	private String bio;
	private String websiteUrl;
	private String jobTitle;
	
}

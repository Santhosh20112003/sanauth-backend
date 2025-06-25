package com.example.demo.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "organization_user_map")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrganizationUserMap {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String orgId;
	
	private String email;
	
	private String role;
	
	private LocalDateTime joinedAt;
	
	
	
}

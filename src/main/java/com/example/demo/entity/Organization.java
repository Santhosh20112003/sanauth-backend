package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organization")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String orgId;

	private String name;

	@Lob
	@Column(columnDefinition = "TEXT", length = 100000000)
	private String photoURL;
	
	@Column(columnDefinition = "TEXT", length = 1000)
	private String description;
	
	private String adminUser;
	
	private LocalDateTime createdAt;

}

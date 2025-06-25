package com.example.demo.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgOptionalData {

	@Id
	private Long id;
	
	private String orgId;
	
	private String industry;
	
	private String website;
	
	private String billingAddress;
	
}

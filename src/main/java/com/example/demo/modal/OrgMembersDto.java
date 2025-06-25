package com.example.demo.modal;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgMembersDto {
	private String userId;
	private String name;
	private String email;
	private String photoUrl;
	private String role;
	private LocalDateTime joinedAt;
}

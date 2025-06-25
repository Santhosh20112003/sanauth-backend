package com.example.demo.modal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddMembersModal {
	private String orgId;
	private String email;
	private String role;
}



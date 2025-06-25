package com.example.demo.modal;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetOrganizationUsers {
	private String orgId;
	private String accessLevel;
	private List<OrgMembersDto> users;
}

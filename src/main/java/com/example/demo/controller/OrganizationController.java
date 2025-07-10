package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.modal.AddMembersModal;
import com.example.demo.modal.OrgCreateModal;
import com.example.demo.modal.OrgIdModal;
import com.example.demo.service.OrganizationService;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/org")
@Slf4j
public class OrganizationController {
	
	@Autowired
	private OrganizationService organizationService;

	@GetMapping("/")
	public String getOrganizationInfo() {
		return "Organization Information: This is a demo organization.";
	}
	
	@PostMapping("/create")
	public ResponseEntity<Object> createNewOrg(@RequestBody OrgCreateModal org, Authentication authentication) {
		log.info("Sample endpoint hit with organization: {}", org.getName());
		return organizationService.createOrganization(org, authentication);
	}
	
	@GetMapping("/get/members/{orgId}")
	public ResponseEntity<Object> getOrgMembers(@PathVariable String orgId, Authentication authentication) {
	    log.info("Fetching organization members for user: {}", authentication.getName());

	    OrgIdModal orgIdModal = new OrgIdModal();
	    orgIdModal.setOrgId(orgId);

	    return organizationService.getOrgMembers(orgIdModal, authentication);
	}
	
	@PostMapping("/add/member")
	public ResponseEntity<Object> addMemberToOrg(@RequestBody AddMembersModal memberEmail, Authentication authentication) {
		log.info("Adding member: {} to organization", memberEmail);
		return organizationService.addMemberToOrg(memberEmail, authentication);
	}
	
	@PostMapping("/update/member")
	public ResponseEntity<Object> updateMemberInOrg(@RequestBody AddMembersModal memberEmail, Authentication authentication) {
		log.info("Updating member: {} in organization", memberEmail);
		return organizationService.updateMemberRoleInOrg(memberEmail, authentication);
	} 
	
	@DeleteMapping("/remove/member")
	public ResponseEntity<Object> removeMemberFromOrg(@RequestBody AddMembersModal memberEmail, Authentication authentication) {
		log.info("Removing member: {} from organization", memberEmail);
		return organizationService.removeMemberFromOrg(memberEmail, authentication);
	}
}

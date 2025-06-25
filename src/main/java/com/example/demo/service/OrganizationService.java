package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Organization;
import com.example.demo.entity.OrganizationUserMap;
import com.example.demo.entity.User;
import com.example.demo.modal.AddMembersModal;
import com.example.demo.modal.GetOrganizationUsers;
import com.example.demo.modal.OrgCreateModal;
import com.example.demo.modal.OrgIdModal;
import com.example.demo.modal.OrgMembersDto;
import com.example.demo.repository.OrgUserMapRepository;
import com.example.demo.repository.OrganizationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.CommonUtils;

@Service
public class OrganizationService {

	@Autowired
	private OrgUserMapRepository orgUserMapRepository;
	
	@Autowired
	private UserRepository userRepository;

	private final OrganizationRepository organizationRepository;

	public OrganizationService(OrganizationRepository organizationRepository) {
		this.organizationRepository = organizationRepository;
	}

	public ResponseEntity<Object> createOrganization(OrgCreateModal org, Authentication authentication) {
		String email = authentication.getName();

		if (organizationRepository.findByName(org.getName()).isPresent()) {
			return new ResponseEntity<>("Organization with this name already exists.", HttpStatus.CONFLICT);
		}

		try {
			Organization organization = new Organization();
			organization.setName(org.getName());
			organization.setDescription(org.getDescription());
			organization.setPhotoURL(org.getPhotoURL());
			organization.setAdminUser(email);
			organization.setCreatedAt(CommonUtils.getLocalDateTime());

			Organization savedOrg = organizationRepository.save(organization);

			return new ResponseEntity<>(savedOrg, HttpStatus.CREATED);

		} catch (Exception e) {
			// Ideally, log the exception here
			return new ResponseEntity<Object>("An error occurred while creating the organization: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<Object> addMemberToOrg(AddMembersModal memberEmail, Authentication authentication) {
		String requesterEmail = authentication.getName();

		// Validate required inputs
		if (isNullOrEmpty(memberEmail.getOrgId()) || isNullOrEmpty(memberEmail.getEmail())
				|| isNullOrEmpty(memberEmail.getRole()) || !CommonUtils.isValidRole(memberEmail.getRole())) {
			return ResponseEntity.badRequest().body(
					"Organization ID, user email, and user role cannot be null or empty, and role must be valid.");
		}

		// Verify organization exists
		Organization organization = organizationRepository.findByOrgId(memberEmail.getOrgId()).orElse(null);

		if (organization == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found.");
		}

		// Verify requester is admin
		if (!organization.getAdminUser().equals(requesterEmail)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("You are not authorized to add members to this organization.");
		}

		// Check if member already exists
		if (orgUserMapRepository.findByOrgIdAndEmail(memberEmail.getOrgId(), memberEmail.getEmail()).isPresent()) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Member with this email already exists in the organization.");
		}

		try {
			OrganizationUserMap newMember = new OrganizationUserMap();
			newMember.setOrgId(memberEmail.getOrgId());
			newMember.setEmail(memberEmail.getEmail());
			newMember.setRole(memberEmail.getRole());
			newMember.setJoinedAt(CommonUtils.getLocalDateTime());

			orgUserMapRepository.save(newMember);

			return ResponseEntity.ok("Member added successfully.");
		} catch (Exception e) {
			// Ideally log the exception
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("An error occurred while adding the member.");
		}
	}

	// Helper method
	private boolean isNullOrEmpty(String value) {
		return value == null || value.trim().isEmpty();
	}

	public ResponseEntity<Object> updateMemberRoleInOrg(AddMembersModal memberEmail, Authentication authentication) {
		String requesterEmail = authentication.getName();

		// Validate required inputs
		if (isNullOrEmpty(memberEmail.getOrgId()) || isNullOrEmpty(memberEmail.getEmail())
				|| isNullOrEmpty(memberEmail.getRole()) || !CommonUtils.isValidRole(memberEmail.getRole())) {
			return ResponseEntity.badRequest()
					.body("Organization ID, user email, and new role cannot be null or empty, and role must be valid.");
		}

		// Verify organization exists
		Organization organization = organizationRepository.findByOrgId(memberEmail.getOrgId()).orElse(null);
		if (organization == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found.");
		}

		// Verify requester is admin
		if (!organization.getAdminUser().equals(requesterEmail)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("You are not authorized to update members in this organization.");
		}

		// Fetch the existing member
		Optional<OrganizationUserMap> existingMemberOpt = orgUserMapRepository
				.findByOrgIdAndEmail(memberEmail.getOrgId(), memberEmail.getEmail());

		if (!existingMemberOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found in the organization.");
		}

		try {
			OrganizationUserMap existingMember = existingMemberOpt.get();

			// Check if the incoming role is the same as the existing one
			if (existingMember.getRole().equalsIgnoreCase(memberEmail.getRole())) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body("The new role is the same as the existing role. No changes were made.");
			}

			existingMember.setRole(memberEmail.getRole());
			orgUserMapRepository.save(existingMember);

			return ResponseEntity.ok("Member role updated successfully.");
		} catch (Exception e) {
			// Ideally log the exception
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("An error occurred while updating the member role.");
		}
	}

	public ResponseEntity<Object> removeMemberFromOrg(AddMembersModal memberEmail, Authentication authentication) {
		String requesterEmail = authentication.getName();

		if (isNullOrEmpty(memberEmail.getOrgId()) || isNullOrEmpty(memberEmail.getEmail())) {
			return ResponseEntity.badRequest().body("Organization ID and user email cannot be null or empty.");
		}

		Organization organization = organizationRepository.findByOrgId(memberEmail.getOrgId()).orElse(null);
		if (organization == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found.");
		}

		if (!organization.getAdminUser().equals(requesterEmail)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("You are not authorized to remove members from this organization.");
		}

		// Prevent removing the admin
		if (memberEmail.getEmail().equalsIgnoreCase(organization.getAdminUser())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You cannot remove the admin of the organization.");
		}

		Optional<OrganizationUserMap> memberOpt = orgUserMapRepository.findByOrgIdAndEmail(memberEmail.getOrgId(),
				memberEmail.getEmail());

		if (!memberOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found in the organization.");
		}

		try {
			orgUserMapRepository.delete(memberOpt.get());
			return ResponseEntity.ok("Member removed successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("An error occurred while removing the member.");
		}
	}

//	public ResponseEntity<Object> getOrgMembers(OrgIdModal orgid, Authentication authentication) {
//	    String requesterEmail = authentication.getName();
//
//	    if (orgid == null || isNullOrEmpty(orgid.getOrgId())) {
//	        return ResponseEntity.badRequest().body("Organization ID cannot be null or empty.");
//	    }
//
//	    Organization organization = organizationRepository.findByOrgId(orgid.getOrgId()).orElse(null);
//	    if (organization == null) {
//	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found.");
//	    }
//
//	    Optional<OrganizationUserMap> requesterMap = orgUserMapRepository.findByOrgIdAndEmail(
//	            orgid.getOrgId(), requesterEmail);
//
//	    if (!organization.getAdminUser().equals(requesterEmail) && requesterMap.isEmpty()) {
//	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
//	                .body("You are not authorized to view members of this organization.");
//	    }
//
//	    // Fetch all members except the requester
//	    List<OrganizationUserMap> members = orgUserMapRepository.findByOrgId(orgid.getOrgId()).stream()
//	            .filter(m -> !m.getEmail().equalsIgnoreCase(requesterEmail))
//	            .collect(Collectors.toList());
//
//	    // Add the admin as a separate entry if not in the orgUserMap
//	    if (!organization.getAdminUser().equalsIgnoreCase(requesterEmail)) {
//	        boolean adminAlreadyInList = members.stream()
//	                .anyMatch(m -> m.getEmail().equalsIgnoreCase(organization.getAdminUser()));
//
//	        if (!adminAlreadyInList) {
//	            OrganizationUserMap admin = new OrganizationUserMap();
//	            admin.setEmail(organization.getAdminUser());
//	            admin.setRole("ADMIN");
//	            admin.setJoinedAt(organization.getCreatedAt() != null ? organization.getCreatedAt() : CommonUtils.getLocalDateTime());
//	            members.add(admin);
//	        }
//	    }
//
//	    // Convert to DTOs
//	    List<OrgMemberDto> memberDtos = members.stream()
//	            .map(m -> new OrgMemberDto(m.getEmail(), m.getRole(), m.getJoinedAt()))
//	            .toList();
//
//	    // Build response
//	    GetOrganizationUsers orgUsers = new GetOrganizationUsers();
//	    orgUsers.setOrgId(orgid.getOrgId());
//	    orgUsers.setAccessLevel(
//	            organization.getAdminUser().equalsIgnoreCase(requesterEmail) ? "ADMIN" : requesterMap.get().getRole());
//	    orgUsers.setUsers(memberDtos);
//
//	    return new ResponseEntity<>(orgUsers, HttpStatus.OK);
//	}
	
	
	public ResponseEntity<Object> getOrgMembers(OrgIdModal orgid, Authentication authentication) {
	    String requesterEmail = authentication.getName();

	    if (orgid == null || isNullOrEmpty(orgid.getOrgId())) {
	        return ResponseEntity.badRequest().body("Organization ID cannot be null or empty.");
	    }

	    Organization organization = organizationRepository.findByOrgId(orgid.getOrgId()).orElse(null);
	    if (organization == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found.");
	    }

	    Optional<OrganizationUserMap> requesterMap = orgUserMapRepository.findByOrgIdAndEmail(
	            orgid.getOrgId(), requesterEmail);

	    if (!organization.getAdminUser().equalsIgnoreCase(requesterEmail) && requesterMap.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body("You are not authorized to view members of this organization.");
	    }

	    // Fetch all mapped users
	    List<OrgMembersDto> allMembers = orgUserMapRepository.findOrgMembersByOrgId(orgid.getOrgId());

	    // Remove requester
	    List<OrgMembersDto> filteredMembers = allMembers.stream()
	            .filter(m -> !m.getEmail().equalsIgnoreCase(requesterEmail))
	            .collect(Collectors.toList());

	    // If admin is not in the orgUserMap, fetch from User and add manually
	    if (!organization.getAdminUser().equalsIgnoreCase(requesterEmail)) {
	        boolean adminAlreadyPresent = filteredMembers.stream()
	                .anyMatch(m -> m.getEmail().equalsIgnoreCase(organization.getAdminUser()));

	        if (!adminAlreadyPresent) {
	            Optional<User> adminUser = userRepository.findByEmail(organization.getAdminUser());
	            if (adminUser.isPresent()) {
	                User u = adminUser.get();
	                OrgMembersDto adminDto = new OrgMembersDto(
	                        u.getUserid(),
	                        u.getName(),
	                        u.getEmail(),
	                        u.getPhotoURL(),
	                        "ADMIN",
	                        organization.getCreatedAt() != null ? organization.getCreatedAt() : CommonUtils.getLocalDateTime()
	                );
	                filteredMembers.add(adminDto);
	            }
	        }
	    }

	    // Build response
	    GetOrganizationUsers response = new GetOrganizationUsers();
	    response.setOrgId(orgid.getOrgId());
	    response.setAccessLevel(
	            organization.getAdminUser().equalsIgnoreCase(requesterEmail) ? "ADMIN" : requesterMap.get().getRole());
	    response.setUsers(filteredMembers);

	    return new ResponseEntity<>(response, HttpStatus.OK);
	}



}

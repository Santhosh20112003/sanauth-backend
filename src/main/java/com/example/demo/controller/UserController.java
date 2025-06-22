package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Organization;
import com.example.demo.entity.User;
import com.example.demo.entity.UserOptionalData;
import com.example.demo.modal.NewAndOldPassword;
import com.example.demo.modal.RefinedLoginHistories;
import com.example.demo.modal.RefinedUserModal;
import com.example.demo.modal.UpdateUserDetailsModal;
import com.example.demo.repository.UserOptionalDataRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UsersService;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

	@Autowired
	private UsersService usersService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserOptionalDataRepository userOptionalDataRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@GetMapping("/get/all")
	public ResponseEntity<List<RefinedUserModal>> getAllUser() {
		log.info("Get all Worked");
		return usersService.getAllUser();
	}

	@GetMapping("/get/login/sessions")
	public ResponseEntity<List<RefinedLoginHistories>> getLoginSession(Authentication authentication) {
		log.info("Fetching login history details");

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		List<RefinedLoginHistories> loginHistories = usersService.getLoginHistory(authentication.getName());

		return loginHistories.isEmpty() ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
				: ResponseEntity.ok(loginHistories);
	}

	@PostMapping("/update")
	public ResponseEntity<String> updateUser(@RequestBody UpdateUserDetailsModal userDetails,
			Authentication authentication) {
		log.info("Attempting to update user: {}", authentication.getName());

		String email = authentication.getName();

		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body("Email is required");
		}

		Optional<User> optionalUser = userRepository.findByEmail(email);
		if (optionalUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
		}

		User user = optionalUser.get();

		// Update User entity fields
		user.setName(userDetails.getName());
		user.setPhotoURL(userDetails.getPhotoURL());

		userRepository.save(user);

		// Update optional user data
		Optional<UserOptionalData> optionalData = userOptionalDataRepository.findByEmail(email);
		UserOptionalData userOptionalData = optionalData.orElseGet(UserOptionalData::new);
		userOptionalData.setEmail(email);
		userOptionalData.setDob(userDetails.getDob());
		userOptionalData.setGender(userDetails.getGender());
		userOptionalData.setPhone(userDetails.getPhone());
		userOptionalData.setLocation(userDetails.getLocation());
		userOptionalData.setBio(userDetails.getBio());
		userOptionalData.setWebsiteUrl(userDetails.getWebsiteUrl());
		userOptionalData.setJobTitle(userDetails.getJobTitle());

		userOptionalDataRepository.save(userOptionalData);

		log.info("User updated successfully for email: {}", email);
		return ResponseEntity.ok("User updated successfully");
	}

	@PostMapping("/create/org")
	public ResponseEntity<String> sendMail(@RequestBody Organization org) {
		log.info("Sample endpoint hit with organization: {}", org.getName());
		return new ResponseEntity<>("Sample endpoint hit", HttpStatus.OK);
	}

	@PostMapping("/change/password")
	public ResponseEntity<String> changePassword(@RequestBody NewAndOldPassword data, Authentication authentication) {
	    log.info("Attempting to change password for user: {}", authentication.getName());

	    String email = authentication.getName();
	    if (email == null || email.isBlank()) {
	        return new ResponseEntity<>("Email is required", HttpStatus.BAD_REQUEST);
	    }
	    
	    if(data.getOld_password() == null || data.getNew_password() == null || data.getOld_password().isBlank() || data.getNew_password().isBlank()) {
	        return new ResponseEntity<>("Old and new passwords are required", HttpStatus.BAD_REQUEST);
	    }

	    Optional<User> optionalUser = userRepository.findByEmail(email);
	    if (optionalUser.isEmpty()) {
	        return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
	    }

	    User user = optionalUser.get();

	    // Check if old password matches
	    if (!passwordEncoder.matches(data.getOld_password(), user.getPassword())) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Old password is incorrect");
	    }

	    // Encode and update new password
	    String encodedNewPassword = passwordEncoder.encode(data.getNew_password());
	    user.setPassword(encodedNewPassword);

	    userRepository.save(user);

	    log.info("Password changed successfully for user: {}", email);
	    return ResponseEntity.ok("Password changed successfully");
	}

}

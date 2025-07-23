package com.example.demo.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.entity.User;
import com.example.demo.entity.UserOptionalData;
import com.example.demo.entity.UserSettings;
import com.example.demo.modal.NewAndOldPassword;
import com.example.demo.modal.RefinedLoginHistories;
import com.example.demo.modal.RefinedUserModal;
import com.example.demo.modal.UpdateUserDetailsModal;
import com.example.demo.repository.UserOptionalDataRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSettingsRepository;
import com.example.demo.service.RedisService;
import com.example.demo.service.TOTPService;
import com.example.demo.service.UsersService;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

	private static final String SESSION_HOST = "http://localhost:9001";

	@Autowired
	private UsersService usersService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserOptionalDataRepository userOptionalDataRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TOTPService totpService;

	@Autowired
	private UserSettingsRepository userSettingsRepository;

	@Autowired
	private RedisService redisService;

	@Autowired
	RestTemplate restTemplate;

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

	@PostMapping("/change/password")
	public ResponseEntity<String> changePassword(@RequestBody NewAndOldPassword data, Authentication authentication) {
		log.info("Attempting to change password for user: {}", authentication.getName());

		String email = authentication.getName();
		if (email == null || email.isBlank()) {
			return new ResponseEntity<>("Email is required", HttpStatus.BAD_REQUEST);
		}

		if (data.getOld_password() == null || data.getNew_password() == null || data.getOld_password().isBlank()
				|| data.getNew_password().isBlank()) {
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

	@PostMapping("/revoke/{token}")
	public ResponseEntity<Map<String, String>> revokeAccess(Authentication authentication,@PathVariable String token) {
		try {
			log.info("Processing logout request");

			callSessionRevokeApi(token);
			log.info("Logout processed successfully");
			return ResponseEntity.ok(Map.of("message", "Revoked successful"));

		} catch (Exception e) {
			log.error("Error during logout: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "An error occurred during logout"));
		} finally {
			log.info("Revoke request completed");
		}
	}
	
	
	
	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
		try {
			log.info("Processing logout request");
			String jwtToken = authentication.getCredentials() instanceof String
					? authentication.getCredentials().toString()
					: null;

			if (jwtToken == null || jwtToken.isBlank()) {
				log.warn("JWT token is null or empty");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("error", "JWT token is required for logout"));
			}

			callSessionRevokeApi(jwtToken);
			log.info("Logout processed successfully");
			return ResponseEntity.ok(Map.of("message", "Logout successful"));

		} catch (Exception e) {
			log.error("Error during logout: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "An error occurred during logout"));
		} finally {
			log.info("Logout request completed");
		}
	}

	public boolean callSessionRevokeApi(String token) throws RestClientException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);

		Map<String, Object> requestBody = new HashMap<>();

		HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

		log.info("Publishing login event to session service for user: {}");
		ResponseEntity<?> response = restTemplate.exchange(SESSION_HOST + "/api/session/revoke/" + token,
				HttpMethod.GET, requestEntity, String.class);

		log.info("Response from session service: {}", response.getStatusCode());

		if (response.getStatusCode() != HttpStatus.OK) {
			log.error("Failed to publish login event to session service: {}", response.getStatusCode());
			return false;
		} else {
			log.info("Login event published successfully to session service");
			return true;
		}
	}
	
	
	@PostMapping("/get/session/list")
	private ResponseEntity<List<Map<String, Object>>> getSessionList(Authentication authentication) {
		log.info("Fetching session list for user: {}", authentication.getName());

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String token = authentication.getCredentials() instanceof String
				? authentication.getCredentials().toString()
				: null;

		if (token == null || token.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
		}

		try {
			List<Map<String, Object>> sessionList = callSessionListApi(token);
			return ResponseEntity.ok(sessionList);
		} catch (RestClientException e) {
			log.error("Error fetching session list: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
		}
	}

	public List<Map<String, Object>> callSessionListApi(String token) throws RestClientException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		log.info("Calling session service to fetch session list");

		ResponseEntity<List> response = restTemplate.exchange(SESSION_HOST + "/api/session/list/all", HttpMethod.POST,
				requestEntity, List.class);

		log.info("Response from session service: {}", response.getStatusCode());

		if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
			log.info("Successfully fetched session list");
			return response.getBody();
		} else {
			log.error("Failed to fetch session list. Status: {}", response.getStatusCode());
			return Collections.emptyList();
		}
	}

	@PostMapping("/register-mfa")
	public ResponseEntity<Object> registerMfa(Authentication authentication) {
		String email = authentication.getName();

		log.info("MFA registration attempt for user: {}", email);

		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Email must not be null or empty"));
		}

		Optional<UserSettings> userSettingsOpt = userSettingsRepository.findByEmail(email);

		if (userSettingsOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User settings not found"));
		}

		UserSettings userSettings = userSettingsOpt.get();

		if (userSettings.isMfaEnabled()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "MFA is already enabled"));
		}

		Map<String, String> registrationData = totpService.register(email);

		if (registrationData == null || registrationData.isEmpty()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Failed to register MFA"));
		}

		userSettings.setMfaEnabled(true);

		userSettingsRepository.save(userSettings);

		log.info("MFA registration successful for user: {}", email);
		return ResponseEntity.ok(registrationData);
	}

	@PostMapping("/verify-mfa-setup")
	public ResponseEntity<Object> verifyMfaSetup(@RequestBody Map<String, String> requestBody,
			Authentication authentication) {
		String email = authentication.getName();
		String code = requestBody.get("code");

		log.info("MFA verification attempt for user: {}", email);

		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Email must not be null or empty"));
		}

		if (code == null || code.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Code must not be null or empty"));
		}

		boolean isValid = totpService.verifyCodeSetup(email, code);

		if (!isValid) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA code"));
		}

		log.info("MFA verification successful for user: {}", email);
		return ResponseEntity.ok(Map.of("message", "MFA setup verified successfully"));
	}

	@GetMapping("/get/settings")
	public ResponseEntity<UserSettings> getUserSettings(Authentication authentication) {
		String email = authentication.getName();

		log.info("Fetching user settings for: {}", email);

		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		Optional<UserSettings> userSettingsOpt = userSettingsRepository.findByEmail(email);

		if (userSettingsOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		UserSettings userSettings = userSettingsOpt.get();
		return ResponseEntity.ok(userSettings);
	}

	@GetMapping("/remove/mfa")
	public ResponseEntity<String> removeMfa(Authentication authentication) {
		String email = authentication.getName();

		log.info("Attempting to remove MFA for user: {}", email);

		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body("Email must not be null or empty");
		}

		Optional<UserSettings> userSettingsOpt = userSettingsRepository.findByEmail(email);

		if (userSettingsOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User settings not found");
		}

		UserSettings userSettings = userSettingsOpt.get();

		if (!userSettings.isMfaEnabled()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("MFA is not enabled for this user");
		}

		userSettings.setMfaEnabled(false);
		userSettingsRepository.save(userSettings);

		totpService.removeSecret(email);

		log.info("MFA removed successfully for user: {}", email);
		return ResponseEntity.ok("MFA removed successfully");
	}

}

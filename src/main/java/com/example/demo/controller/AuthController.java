package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.JwtUtil;
import com.example.demo.config.SecurityConfig;
import com.example.demo.entity.User;
import com.example.demo.entity.UserLoginHistory;
import com.example.demo.entity.UserOptionalData;
import com.example.demo.entity.UserOtp;
import com.example.demo.entity.UserSettings;
import com.example.demo.modal.EmailAndOtp;
import com.example.demo.modal.EmailOtpAndPassword;
import com.example.demo.modal.LoginMFARequiredModal;
import com.example.demo.modal.MagicLinkPayload;
import com.example.demo.modal.MailModal;
import com.example.demo.modal.RefinedUserModal;
import com.example.demo.modal.UserFullDetailsModal;
import com.example.demo.modal.UserNameAndPasswordWithMetaData;
import com.example.demo.modal.UserNamePassword;
import com.example.demo.modal.WebSocketPayload;
import com.example.demo.repository.LoggingRepository;
import com.example.demo.repository.OtpRepository;
import com.example.demo.repository.UserOptionalDataRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSettingsRepository;
import com.example.demo.service.NotificationService;
import com.example.demo.service.TOTPService;
import com.example.demo.service.UsersService;
import com.example.demo.util.CommonUtils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

	@Autowired
	private UsersService usersService;

	@Autowired
	private LoggingRepository loggingRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtutil;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OtpRepository otpRepository;

	@Autowired
	private UserOptionalDataRepository userOptionalDataRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private UserSettingsRepository userSettingsRepository;

	@Autowired
	private TOTPService totpService;

	@PostConstruct
	public void init() {
		log.info("AuthController initialized");
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/create")
	public ResponseEntity<User> createUser(@RequestBody User user) {
		try {
			log.info("POST /api/auth/admin/create - User creation initiated");

			// Validate input
			if (user.getEmail() == null || user.getPassword() == null) {
				log.warn("Email or password is missing in the request");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			if (userRepository.findByEmail(user.getEmail()).isPresent()) {
				log.info("User with email {} already exists", user.getEmail());
				return new ResponseEntity<>(HttpStatus.CONFLICT);
			}

			// Save user settings
			UserSettings userSettings = new UserSettings();
			userSettings.setEmail(user.getEmail());
			userSettingsRepository.save(userSettings);

			// Prepare and save user
			user.setActive(true);
			user.setPassword(passwordEncoder.encode(user.getPassword()));

			return usersService.createUser(user);
		} catch (Exception e) {
			log.error("Error creating user: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/sample")
	public ResponseEntity<String> sampleCall() {
		return new ResponseEntity<>("Sample Response from Admin", HttpStatus.OK);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/send-magic-link")
	public ResponseEntity<Object> sendMagicLink(HttpServletRequest request, @RequestBody MagicLinkPayload user) {
		String email = user.getEmail();
		log.info("Sending magic link to email: {}", email);

		// Validate email input
		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Email must not be null or empty"));
		}

		if (!CommonUtils.isValidEmail(email)) {
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
		}

		// Create login metadata
		UserLoginHistory metadata = new UserLoginHistory();
		metadata.setDeviceInfo(user.getMetadata().getDeviceInfo());
		metadata.setIpAddress(user.getMetadata().getIpAddress());
		metadata.setLocation(user.getMetadata().getLocation());
		metadata.setLoginTime(CommonUtils.getLocalDateTime());
		metadata.setEmail(user.getEmail());
		log.info("Login attempt for user: {}", user.getEmail());

		try {
			// Fetch user from the database
			Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
			if (optionalUser.isEmpty()) {
				metadata.setStatus("UNVERIFIED");
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("error", "User not found with the provided email"));
			}

			User dbUser = optionalUser.get();
			metadata.setStatus("SUCCESS");

			// Update user's last login time
			dbUser.setLastLogin(CommonUtils.getLocalDateTime());
			userRepository.save(dbUser);

			// Send magic link
			boolean magicLinkSent = usersService.sendMagicLink(email);
			if (magicLinkSent) {
				return ResponseEntity.ok(Map.of("message", "Magic link sent to " + email));
			} else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(Map.of("error", "Failed to send magic link"));
			}
		} catch (Exception e) {
			log.error("Error sending magic link: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "An error occurred while sending the magic link"));
		}
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/verify-mfa")
	public ResponseEntity<Map<String, String>> verifyMfa(@RequestBody UserNameAndPasswordWithMetaData user) {
		log.info("MFA verification attempt for user: {}", user.getEmail());

		if (user.getEmail() == null || user.getOtp() == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Email and Password must not be null"));
		}

		boolean isValid = totpService.verifyCode(user.getEmail(), user.getOtp());

		System.out.println("MFA verification result for user " + user.getEmail() + ": " + isValid);
		
		if (isValid) {

			// Authenticate user
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
			UserDetails userDetails = (UserDetails) authentication.getPrincipal();

			// Generate JWT token
			String token = jwtutil.generateToken(userDetails.getUsername(), userDetails.getAuthorities().toString());

			log.info("MFA verification successful for user: {}", user.getEmail());
			return ResponseEntity.ok(Map.of("token", token, "message", "MFA verification successful"));

		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or Reused OTP"));
		}

	}


	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(HttpServletRequest request,
			@RequestBody UserNameAndPasswordWithMetaData user) {
		UserLoginHistory metadata = new UserLoginHistory();
		log.info("Login attempt for user: {}", user.getEmail());

		metadata.setDeviceInfo(user.getMetadata().getDeviceInfo());
		metadata.setIpAddress(user.getMetadata().getIpAddress());
		metadata.setLocation(user.getMetadata().getLocation());
		metadata.setLoginTime(CommonUtils.getLocalDateTime());
		metadata.setEmail(user.getEmail());

		try {
			if (user.getEmail() == null || user.getPassword() == null) {
				metadata.setStatus("FAILED");
				return ResponseEntity.badRequest().body(Map.of("error", "Email and Password must not be null"));
			}

			// Authenticate user
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
			UserDetails userDetails = (UserDetails) authentication.getPrincipal();

			// Fetch user from DB
			Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
			if (optionalUser.isEmpty()) {
				metadata.setStatus("UNVERIFIED");
				return handleUnverifiedUser(user.getEmail(), metadata);
			}

			User dbUser = optionalUser.get();

			if (!dbUser.isEmailVerified()) {
				metadata.setStatus("UNVERIFIED");
				return handleUnverifiedUser(user.getEmail(), metadata);
			}

			if (!dbUser.isActive()) {
				metadata.setStatus("INACTIVE");
				return new ResponseEntity<>(Map.of("error", "User is inactive. Please contact support."),
						HttpStatus.FORBIDDEN);
			}

			// MFA Check

			Optional<UserSettings> userSettingsOptional = userSettingsRepository.findByEmail(user.getEmail());

			if (userSettingsOptional.isEmpty()) {
				// User settings not found, create default settings
				UserSettings userSettings = new UserSettings();
				userSettings.setEmail(user.getEmail());
				userSettingsRepository.save(userSettings);
			}

			if (userSettingsOptional.isPresent() && userSettingsOptional.get().isMfaEnabled()) {
				// MFA is enabled, handle accordingly
				metadata.setStatus("MFA_REQUIRED");
				return new ResponseEntity<>(
						Map.of("response", new LoginMFARequiredModal("Multi-Factor Authentication is required", true)),
						HttpStatus.PARTIAL_CONTENT);
			}

			// Successful login
			String token = jwtutil.generateToken(userDetails.getUsername(), userDetails.getAuthorities().toString());
			metadata.setStatus("SUCCESS");

			dbUser.setLastLogin(CommonUtils.getLocalDateTime());
			userRepository.save(dbUser);

			log.info("User last login updated: {}", dbUser.getLastLogin());

			return ResponseEntity.ok(Map.of("token", token));

		} catch (Exception e) {
			metadata.setStatus("FAILED");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
		} finally {
			loggingRepository.save(metadata);
			log.info("Login processed for user: {}", user.getEmail());
		}
	}

	private ResponseEntity<Map<String, Object>> handleUnverifiedUser(String email, UserLoginHistory metadata) {
		Optional<UserOtp> existingOtp = otpRepository.findTopByEmailAndTypeAndUsedOrderByCreatedAtDesc(email,
				"VERIFICATION", false);

		if (existingOtp.isPresent() && !UserOtp.isExpired(existingOtp.get().getCreatedAt(), 60)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("message", "An OTP has already been sent. Please check your email."));
		}

		boolean emailSent = usersService.sendVerificationEmail(email, "VERIFICATION");

		if (emailSent) {
			return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
					.body(Map.of("response", "User is not verified. A verification email has been sent to " + email));
		} else {
			metadata.setStatus("UNVERIFIED_EMAIL_FAILED");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("error", "User is not verified and email sending failed"));
		}
	}

	@PostMapping("/verify")
	public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody EmailAndOtp request) {
		String email = request.getEmail();
		Integer otp = request.getOtp();

		// Validate inputs
		if (email == null || otp == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Email and OTP must not be null"));
		}

		// Fetch latest unused OTP for verification
		Optional<UserOtp> otpOptional = otpRepository.findTopByEmailAndTypeAndUsedOrderByCreatedAtDesc(email,
				"VERIFICATION", false);

		if (otpOptional.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "No OTP found for the provided email"));
		}

		UserOtp userOtp = otpOptional.get();

		// Check if OTP matches
		if (!userOtp.getOtp().equals(otp)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid OTP"));
		}

		// Check if OTP is expired
		if (UserOtp.isExpired(userOtp.getCreatedAt(), 60)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "OTP has expired"));
		}

		// Mark OTP as used
		userOtp.setUsed(true);
		otpRepository.save(userOtp);

		// Verify user's email
		Optional<User> userOptional = userRepository.findByEmail(email);
		if (userOptional.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
		}

		User user = userOptional.get();
		if (user.isEmailVerified()) {
			return ResponseEntity.ok(Map.of("message", "Email is already verified"));
		}

		user.setEmailVerified(true);
		userRepository.save(user);

		return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
	}

	@PostMapping("/resend-otp")
	public ResponseEntity<Map<String, String>> resendOtp(@RequestBody MailModal request) {
		String email = request.getEmail();

		if (email == null) {
			return new ResponseEntity<>(Map.of("error", "Email must not be null"), HttpStatus.BAD_REQUEST);
		}

		Optional<User> userOptional = userRepository.findByEmail(email);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			if (user.isEmailVerified()) {
				return new ResponseEntity<>(Map.of("message", "Email is already verified"), HttpStatus.CREATED);
			}
			if (!user.isEmailVerified()) {
				boolean emailSent = usersService.sendVerificationEmail(email, "VERIFICATION");
				if (emailSent) {
					return new ResponseEntity<>(Map.of("message", "Verification email sent successfully"),
							HttpStatus.OK);
				} else {
					return new ResponseEntity<>(Map.of("error", "Failed to send verification email"),
							HttpStatus.INTERNAL_SERVER_ERROR);
				}
			} else {
				return new ResponseEntity<>(Map.of("message", "Email is already verified"), HttpStatus.OK);
			}
		} else {
			return new ResponseEntity<>(Map.of("error", "User not found with the provided email"),
					HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody MailModal request) {
		String email = request.getEmail();

		if (email == null) {
			return new ResponseEntity<>(Map.of("error", "Email must not be null"), HttpStatus.BAD_REQUEST);
		}

		Optional<User> userOptional = userRepository.findByEmail(email);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			boolean emailSent = usersService.sendResetPasswordEmail(email);
			if (emailSent) {
				return new ResponseEntity<>(Map.of("message", "Reset password email sent successfully"), HttpStatus.OK);
			} else {
				return new ResponseEntity<>(Map.of("error", "Failed to send reset password email"),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return new ResponseEntity<>(Map.of("error", "User not found with the provided email"),
					HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Map<String, String>> resetPassword(@RequestBody EmailOtpAndPassword request) {
		String email = request.getEmail();
		Integer otp = request.getOtp();
		String newPassword = request.getPassword();

		// Validate input
		if (email == null || otp == null || newPassword == null || newPassword.isBlank()) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "Email, OTP, and password must not be null or empty"));
		}

		try {
			// Find user by email
			Optional<User> optionalUser = userRepository.findByEmail(email);
			if (optionalUser.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("error", "User not found with the provided email"));
			}

			User user = optionalUser.get();

			// Find latest unused OTP of type RESET_PASSWORD
			Optional<UserOtp> otpOptional = otpRepository.findTopByEmailAndTypeAndUsedOrderByCreatedAtDesc(email,
					"RESET_PASSWORD", false);

			if (otpOptional.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("error", "No OTP found or already used"));
			}

			UserOtp userOtp = otpOptional.get();

			// Check OTP match
			if (!otp.equals(userOtp.getOtp())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid OTP"));
			}

			// Optionally: Check OTP expiry
			if (UserOtp.isExpired(userOtp.getCreatedAt(), 600)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "OTP has expired"));
			}

			// Mark OTP as used
			userOtp.setUsed(true);
			otpRepository.save(userOtp);

			// Update user password
			user.setPassword(passwordEncoder.encode(newPassword));
			userRepository.save(user);

			// Send confirmation email
			usersService.sendPasswordChange(email);

			return ResponseEntity.ok(Map.of("message", "Password reset successfully"));

		} catch (Exception e) {
			e.printStackTrace(); // Consider using a proper logger
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "An internal error occurred while resetting the password"));
		}
	}

	@GetMapping("/me")
	public ResponseEntity<UserFullDetailsModal> getCurrentUser(Authentication authentication) {
		log.info("Fetching current user details");

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String email = authentication.getName();
		Optional<User> userOptional = userRepository.findByEmail(email);
		Optional<UserOptionalData> optionalUserData = userOptionalDataRepository.findByEmail(email);

		if (userOptional.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		User user = userOptional.get();
		UserFullDetailsModal userDetails = new UserFullDetailsModal();

		userDetails.setUid(user.getUid());
		userDetails.setEmail(user.getEmail());
		userDetails.setName(user.getName());
		userDetails.setPhotoURL(user.getPhotoURL());
		userDetails.setRole(user.getRole());
		userDetails.setCreatedAt(CommonUtils.getLocalDateTime());
		userDetails.setLastLogin(user.getLastLogin());
		userDetails.setActive(user.isActive());
		userDetails.setEmailVerified(user.isEmailVerified());

		optionalUserData.ifPresent(userData -> {
			userDetails.setBio(userData.getBio());
			userDetails.setPhone(userData.getPhone());
			userDetails.setDob(userData.getDob());
			userDetails.setGender(userData.getGender());
			userDetails.setLocation(userData.getLocation());
			userDetails.setWebsiteUrl(userData.getWebsiteUrl());
			userDetails.setJobTitle(userData.getJobTitle());
		});
//		notificationService.sendToClients(new WebSocketPayload(email,"","","User details fetched for: " + email),email);
		return ResponseEntity.ok(userDetails);
	}

}

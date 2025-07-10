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
import com.example.demo.modal.EmailAndOtp;
import com.example.demo.modal.EmailOtpAndPassword;
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
import com.example.demo.service.NotificationService;
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

	@PostConstruct
	public void init() {
		log.info("AuthController initialized");
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/create")
	public ResponseEntity<User> createUser(@RequestBody User user) {
		log.info("creation /api/auth/create route started");
		user.setActive(true);
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return usersService.createUser(user);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/sample")
	public ResponseEntity<String> sampleCall() {
		return new ResponseEntity<>("Sample Response from Admin", HttpStatus.OK);
	}

//	@PostMapping("/login")
//	public ResponseEntity<Map<String, String>> login(HttpServletRequest request,@RequestBody UserNameAndPasswordWithMetaData user) {
//		UserLoginHistory metadata = new UserLoginHistory();
//		System.out.println("IP ADDRESS: "+request.getRemoteAddr());
//		
//		metadata.setDeviceInfo(user.getMetadata().getDeviceInfo());
//		metadata.setIpAddress(user.getMetadata().getIpAddress());
//		metadata.setLocation(user.getMetadata().getLocation());
//		metadata.setLoginTime(user.getMetadata().getLoginTime());
//		metadata.setEmail(user.getEmail());
//		try {
//			log.info("Login attempt for user: {}", user.getEmail());
//				
//			if (user.getEmail() == null || user.getPassword() == null) {
//				return new ResponseEntity<>(Map.of("error", "Email and Password must not be null"), HttpStatus.BAD_REQUEST);
//			}
//			
//			Authentication authentication = authenticationManager
//					.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
//			UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//            
//			return userRepository.findByEmail(user.getEmail()).filter(User::isEmailVerified).map(verifiedUser -> {
//				userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
//					if(!u.isActive()) {
//						metadata.setStatus("INACTIVE");
//						loggingRepository.save(metadata);
//						throw new RuntimeException("User is inactive. Please contact support.");
//					}
//				});
//				String token = jwtutil.generateToken(userDetails.getUsername(),
//						userDetails.getAuthorities().toString());
//				metadata.setStatus("SUCCESS");
//                userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
//					u.setLastLogin(metadata.getLoginTime());
//					userRepository.save(u);
//					System.out.println("User last login updated: " + u.getLastLogin());
//				});
//				return ResponseEntity.ok(Map.of("token", token));
//			}).orElseGet(() -> {
//				
//				Optional<UserOtp> existingOtp = otpRepository.findTopByEmailAndTypeAndUsedOrderByCreatedAtDesc(user.getEmail(), "VERIFICATION", false);
//
//				if (existingOtp.isPresent() && !UserOtp.isExpired(existingOtp.get().getCreatedAt(), 60)) {
//				    return ResponseEntity.status(HttpStatus.CONFLICT)
//				        .body(Map.of("message", "An OTP has already been sent. Please check your email."));
//				}
//				
//				boolean emailSent = usersService.sendVerificationEmail(user.getEmail(),"VERIFICATION");
//				if (emailSent) {
//					metadata.setStatus("UNVERIFIED");
//					return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("response",
//							"User is not verified. A verification email has been sent to " + user.getEmail()));
//				} else {
//					metadata.setStatus("UNVERIFIED_EMAIL_FAILED");
//					return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//							.body(Map.of("error", "User is not verified and email sending failed"));
//				}
//			});
//
//		} catch (Exception e) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
//		}
//		finally {
//			
//			if(metadata.getStatus() == null) {
//				metadata.setStatus("FAILED");
//			}
//			
//			loggingRepository.save(metadata);
//			log.info("Login for user: {}", user.getEmail());
//		}
//	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, String>> login(HttpServletRequest request,
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
				return new ResponseEntity<>(Map.of("error", "User is inactive. Please contact support."), HttpStatus.FORBIDDEN);
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

	private ResponseEntity<Map<String, String>> handleUnverifiedUser(String email, UserLoginHistory metadata) {
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

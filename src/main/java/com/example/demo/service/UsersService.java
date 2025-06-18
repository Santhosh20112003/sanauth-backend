package com.example.demo.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserLoginHistory;
import com.example.demo.entity.UserOtp;
import com.example.demo.modal.RefinedLoginHistories;
import com.example.demo.modal.RefinedUserModal;
import com.example.demo.repository.LoggingRepository;
import com.example.demo.repository.OtpRepository;
import com.example.demo.repository.UserRepository;

@Service
public class UsersService {
	@Autowired
	private OtpRepository otpRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private LoggingRepository loggingRepository;

	public ResponseEntity<User> createUser(User user) {
		return new ResponseEntity(userRepository.save(user), HttpStatus.CREATED);
	}

	public ResponseEntity<List<RefinedUserModal>> getAllUser() {
		List<User> users = userRepository.findAll();
		List<RefinedUserModal> refinedUsers = users.stream().map(user -> new RefinedUserModal(user)) // assumes a
																										// constructor
																										// exists
				.toList();

		return new ResponseEntity<>(refinedUsers, HttpStatus.OK);

	}

	public boolean sendVerificationEmail(String email, String type) {
		try {
			// Generate a 4-digit OTP
			int otp = (int) (Math.random() * 9000) + 1000;

			// Update OTP in the database
			UserOtp rowsUpdated = otpRepository
					.save(new UserOtp(null, email, otp, type, Instant.now(), false));
			if (rowsUpdated == null) {
				System.out.println("No user found with email: " + email);
				return false;
			}
			

			// Prepare and send the email
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(email);
			message.setSubject("SanAuth Email Verification");
			message.setText("Hello,\n\n" + "Your verification code is: " + otp + "\n\n"
					+ "Please use this code to verify your account.\n\n" + "Thank you,\nSanAuth Team");

			System.out.println("Sending email to: " + email);
			mailSender.send(message);

			return true;
		} catch (Exception e) {
			System.err.println("Error sending verification email: " + e.getMessage());
			return false;
		}
	}

	public boolean sendResetPasswordEmail(String email) {
		try {
			// Generate a 4-digit OTP
			int otp = (int) (Math.random() * 9000) + 1000;

			// Update OTP in the database
			UserOtp rowsUpdated = otpRepository
					.save(new UserOtp(null, email, otp, "RESET_PASSWORD", Instant.now(), false));
			if (rowsUpdated == null) {
				System.out.println("No user found with email: " + email);
				return false;
			}

			// Prepare and send the email
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(email);
			message.setSubject("SanAuth Password Reset Verification");
			message.setText("Hello,\n\n" + "Your password reset verification code is: " + otp + "\n\n"
					+ "Please use this code to reset your password.\n\n" + "Thank you,\nSanAuth Team");

			System.out.println("Sending email to: " + email);
			mailSender.send(message);

			return true;
		} catch (Exception e) {
			System.err.println("Error sending verification email: " + e.getMessage());
			return false;
		}

	}

	public boolean sendPasswordChange(String email) {
		try {
			// Prepare and send the email
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(email);
			message.setSubject("SanAuth Password Change Notification");
			message.setText("Hello,\n\n" + "Your password has been successfully changed.\n\n"
					+ "If you did not initiate this change, please contact support immediately.\n\n"
					+ "Thank you,\nSanAuth Team");
			System.out.println("Sending email to: " + email);
			mailSender.send(message);
			return true;
		} catch (Exception e) {
			System.err.println("Error sending verification email: " + e.getMessage());
			return false;
		}

	}

	public List<RefinedLoginHistories> getLoginHistory(String name) {
		try {
			return loggingRepository.findByEmail(name).stream()
					.map(history -> new RefinedLoginHistories(history.getLoginTime(), history.getDeviceInfo(),
							history.getIpAddress(), history.getLocation(), history.getStatus()))
					.collect(Collectors.toList());
		} catch (Exception e) {
			System.err.println("Error fetching login history: " + e.getMessage());
			return null;
		}
	}

}

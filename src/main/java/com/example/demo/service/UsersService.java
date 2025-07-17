package com.example.demo.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.demo.config.JwtUtil;
import com.example.demo.entity.User;
import com.example.demo.entity.UserOtp;
import com.example.demo.modal.RefinedLoginHistories;
import com.example.demo.modal.RefinedUserModal;
import com.example.demo.repository.LoggingRepository;
import com.example.demo.repository.OtpRepository;
import com.example.demo.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UsersService {
	@Autowired
	private OtpRepository otpRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private LoggingRepository loggingRepository;

	public ResponseEntity<User> createUser(User user) {
		return new ResponseEntity<User>(userRepository.save(user), HttpStatus.CREATED);
	}

	public ResponseEntity<List<RefinedUserModal>> getAllUser() {
		List<User> users = userRepository.findAll();
		List<RefinedUserModal> refinedUsers = users.stream().map(user -> new RefinedUserModal(user)).toList();
		return new ResponseEntity<>(refinedUsers, HttpStatus.OK);
	}

	public boolean sendVerificationEmail(String email, String type) {
		try {
			// Generate a 4-digit OTP
			int otp = (int) (Math.random() * 9000) + 1000;

			// Update OTP in the database
			UserOtp rowsUpdated = otpRepository.save(new UserOtp(null, email, otp, type, Instant.now(), false));
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

	public boolean sendMagicLink(String email) {
		// Check if user exists
		Optional<User> userOpt = userRepository.findByEmail(email);
		if (userOpt.isEmpty()) {
			log.info("❌ User not found for email: {}", email);
			return false;
		}

		User user = userOpt.get();

		// Check if user is verified
		if (!user.isEmailVerified()) {
			log.info("❌ User not verified for email: {}", email);
			return false;
		}

		// Generate token
		String token = jwtUtil.generateToken(email, user.getRole());
		if (token == null) {
			log.info("❌ Failed to generate token for email: {}", email);
			return false;
		}

		// Compose magic link
		String magicLink = "http://localhost:5173/magic-link/" + token;

		// Send email
		try {
			// Create MIME message
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

			// Build HTML content
			String htmlContent = String.format(
				    """
				    <html>
				        <head>
				            <style>
				                body {
				                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
				                    background-color: #f2f3f8;
				                    color: #444;
				                    margin: 0;
				                    padding: 0;
				                }

				                .email-wrapper {
				                    width: 100%%;
				                    padding: 40px 0;
				                    background-color: #f2f3f8;
				                }

				                .email-container {
				                    max-width: 600px;
				                    background-color: #ffffff;
				                    margin: 0 auto;
				                    padding: 40px 30px;
				                    border-radius: 10px;
				                    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
				                }

				                h2 {
				                    color: #2c3e50;
				                    margin-bottom: 16px;
				                }

				                p {
				                    font-size: 16px;
				                    line-height: 1.6;
				                }

				                .btn {
				                    display: inline-block;
				                    margin-top: 25px;
				                    padding: 14px 30px;
				                    background: linear-gradient(135deg, #4e9af1, #3368d6);
				                    color: #ffffff !important;
				                    text-decoration: none;
				                    font-weight: bold;
				                    font-size: 16px;
				                    border-radius: 8px;
				                    transition: background 0.3s ease;
				                }

				                .btn:hover {
				                    background: linear-gradient(135deg, #3a87e0, #2755c5);
				                }

				                .footer {
				                    margin-top: 40px;
				                    font-size: 13px;
				                    color: #999;
				                    border-top: 1px solid #eee;
				                    padding-top: 20px;
				                }

				                .brand {
				                    font-size: 28px;
				                    font-weight: bold;
				                    color: #007bff;
				                    margin-bottom: 10px;
				                }
				            </style>
				        </head>
				        <body>
				            <div class="email-wrapper">
				                <div class="email-container">
				                    <div class="brand">🔐 SanAuth</div>
				                    <h2>Magic Login Link</h2>
				                    <p>Hi %s,</p>
				                    <p>Click the button below to securely log in to your account. This login link is valid for only <strong>15 minutes</strong>.</p>
				                    <a class="btn" href="%s">Access My Account</a>
				                    <p>If you did not request this login, feel free to ignore this email.</p>
				                    <div class="footer">
				                        <p>Thank you,<br><strong>SanAuth Team</strong></p>
				                        <p>Need help? Contact support at <a href="mailto:support@sanauth.com">support@sanauth.com</a></p>
				                    </div>
				                </div>
				            </div>
				        </body>
				    </html>
				    """,
				    email, magicLink
				);


			// Set email details
			helper.setTo(email);
			helper.setSubject("SanAuth Magic Link");
			helper.setText(htmlContent, true); // true = send as HTML

			mailSender.send(mimeMessage);

			System.out.printf("✅ Magic link sent to: %s%n", email);
			return true;

		} catch (Exception e) {
			System.err.printf("❌ Error sending magic link to %s: %s%n", email, e.getMessage());
			return false;
		}

	}

}

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.User;
import com.example.demo.entity.UserLoginHistory;
import com.example.demo.modal.MailModal;
import com.example.demo.modal.RefinedLoginHistories;
import com.example.demo.modal.RefinedUserModal;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UsersService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

	    return loginHistories.isEmpty()
	            ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
	            : ResponseEntity.ok(loginHistories);
	}

}

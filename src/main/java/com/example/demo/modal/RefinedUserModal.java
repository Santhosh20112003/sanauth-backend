package com.example.demo.modal;

import java.time.Instant;
import java.time.LocalDateTime;

import com.example.demo.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefinedUserModal {
	private String uid;
    private String email;
    private String name;
    private String photoURL;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean isActive;
    
    public RefinedUserModal(User user) {
	    this.uid = user.getUid();
	    this.email = user.getEmail();
	    this.name = user.getName();
	    this.photoURL = user.getPhotoURL(); 
	    this.role = user.getRole();
	    this.createdAt = user.getCreatedAt();
	    this.lastLogin = user.getLastLogin();
	    this.isActive = user.isActive();
	}
    
}

	package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "userid", updatable = false, nullable = false) 
    private String userid;

    @Column(unique = true, nullable = false)
    private String uid; 

    @Column(unique = true,nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;
 
    @Lob
    @Column(columnDefinition = "TEXT",length = 100000000)
    private String photoURL;

    @Column(nullable = false)
    private String role;

    private Instant createdAt;

    private Instant lastLogin;
  
    private boolean isActive;
    
    private boolean isEmailVerified;
    
//    @Column(nullable = true)
//    private Integer otp;
//    
//    @Column(name = "forgot_password_otp",nullable = true)
//    private Integer forgot_password_otp;
    
    @ManyToMany
    @JoinTable(
        name = "user_organization",
        joinColumns = @JoinColumn(name = "userid"),
        inverseJoinColumns = @JoinColumn(name = "org_id")
    )
    private List<Organization> organizations;

    
    
    
}

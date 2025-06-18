package com.example.demo.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;


@Component
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("Loading user by email: " + email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
        
        System.out.println("User found: " + user.getEmail());

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), // username
            user.getPassword(), // password 
            Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_"+user.getRole())
            )
        );
    }
}

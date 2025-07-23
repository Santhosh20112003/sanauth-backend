package com.example.demo.config;

import com.example.demo.modal.Payload;
import com.example.demo.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("JwtFilter is processing the request");

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        	response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT Not Found in Request Header");
            return;
        }

        try {
            String jwtToken = authHeader.substring(7);
            System.out.println("JWT Token: " + jwtToken);
            
            if(jwtUtil.isTokenExpired(jwtToken)) {
            	response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "JWT Token Expired");
            	return;
            }
            
            Payload payload = jwtUtil.extractPayload(jwtToken);
            System.out.println("Extracted Payload: " + payload);

            if (payload != null && payload.getEmail() != null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(payload.getEmail());
                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails.getUsername(),
                                    jwtToken,
                                    userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    System.out.println("Authentication set in SecurityContext");
                } else {
                    System.out.println("Invalid JWT token");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        boolean shouldSkip = !path.startsWith("/api/users/") && 
							 !path.startsWith("/api/auth/me") && !path.startsWith("/api/org/");

        if (shouldSkip) {
            System.out.println("Skipping JwtFilter for path: " + path);
        }

        return shouldSkip;
    }
}

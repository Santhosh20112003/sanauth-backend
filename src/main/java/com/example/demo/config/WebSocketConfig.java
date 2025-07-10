package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic"); // Enable a simple in-memory message broker for topics
		config.setApplicationDestinationPrefixes("/app"); // Prefix for messages that are bound for methods annotated with @MessageMapping	
	}
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws") // Endpoint for WebSocket connection
		        .setAllowedOrigins("http://localhost:9090/" ,"http://localhost:5173/") // Allow all origins for CORS
		        .withSockJS(); // Enable SockJS fallback options
	}
}

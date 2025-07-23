package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = "com.example.demo")
public class SanauthTestingApplication {
	public static void main(String[] args) {
		SpringApplication.run(SanauthTestingApplication.class, args);
	}
	
	
	@Bean
	RestTemplate getRestTemplate() {
		return new RestTemplate();
	}
	
}

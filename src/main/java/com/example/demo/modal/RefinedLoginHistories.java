package com.example.demo.modal;

import java.time.Instant;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class RefinedLoginHistories {
	private LocalDateTime loginTime;
	private String deviceInfo;
	private String ipAddress;
	private String location;
	private String status;
}

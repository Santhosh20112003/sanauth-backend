package com.example.demo.modal;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class RefinedLoginHistories {
	private Instant loginTime;
	private String deviceInfo;
	private String ipAddress;
	private String location;
	private String status;
}

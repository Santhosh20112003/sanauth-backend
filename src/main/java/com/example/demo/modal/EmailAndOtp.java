package com.example.demo.modal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailAndOtp {
	private String email;
	private Integer otp;
}

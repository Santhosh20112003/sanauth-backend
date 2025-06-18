package com.example.demo.modal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailOtpAndPassword {
	private String email;
	private Integer otp;
	private String password;
}

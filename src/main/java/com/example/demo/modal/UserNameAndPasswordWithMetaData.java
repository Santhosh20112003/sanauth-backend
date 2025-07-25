package com.example.demo.modal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserNameAndPasswordWithMetaData {
	private String email;
	private String password;
	private String otp;
	private LoginHistoryRequest metadata;
}

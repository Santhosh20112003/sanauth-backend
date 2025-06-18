package com.example.demo.modal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class ResponseModal {
	private int status;
	private Object data;
}

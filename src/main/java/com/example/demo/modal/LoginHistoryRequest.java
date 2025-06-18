package com.example.demo.modal;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryRequest {
    private Instant loginTime;
    private String deviceInfo;
    private String ipAddress;
    private String location;
}
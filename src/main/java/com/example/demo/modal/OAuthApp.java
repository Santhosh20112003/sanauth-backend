package com.example.demo.modal;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OAuthApp {
    private Long id;
    private String appId;
    private Instant createdAt;

}

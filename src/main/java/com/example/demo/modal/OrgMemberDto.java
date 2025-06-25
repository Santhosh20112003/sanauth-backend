package com.example.demo.modal;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgMemberDto {
    private String email;
    private String role;
    private LocalDateTime joinedAt;
}
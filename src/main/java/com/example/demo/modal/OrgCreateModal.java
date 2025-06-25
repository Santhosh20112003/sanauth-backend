package com.example.demo.modal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgCreateModal {
 private String name;
 private String description;
 private String photoURL;
}

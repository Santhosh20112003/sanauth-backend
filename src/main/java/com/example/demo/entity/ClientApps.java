package com.example.demo.entity;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@Data
@AllArgsConstructor
@Table(name = "client_apps")
public class ClientApps {

	@Id
	private String appId;
	private String uid;
	private String appName;
	private String appImageUrl;
	private String appDescription;
	private String clientId;
	private String clientSecret;
	private List<String> redirectUris;
	private Date createdAt;
	private Date updatedAt;

//	@ManyToOne
//	@JoinColumn(name = "organization_id")
//	private Organization organization;

}

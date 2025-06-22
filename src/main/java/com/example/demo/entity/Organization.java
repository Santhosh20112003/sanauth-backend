package com.example.demo.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organization")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String orgId;

	private String name;

	@Lob
	@Column(columnDefinition = "TEXT", length = 100000000)
	private String photoURL;

	private String adminUser;

//	@ManyToMany(mappedBy = "organizations", fetch = FetchType.LAZY)
//	@JsonIgnore
//	private List<User> users = new ArrayList();
//
//	@OneToMany(mappedBy = "organizations", fetch = FetchType.LAZY)
//	private List<ClientApps> apps = new ArrayList();

}

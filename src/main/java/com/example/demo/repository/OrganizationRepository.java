package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, String> {

	public Optional<Organization> findByName(String name);

	public Optional<Organization> findByOrgId(String orgId);
	
}

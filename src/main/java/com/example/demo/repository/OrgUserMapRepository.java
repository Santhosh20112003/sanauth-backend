package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.OrganizationUserMap;
import com.example.demo.modal.OrgMembersDto;

public interface OrgUserMapRepository extends JpaRepository<OrganizationUserMap, Long> {

	Optional<OrganizationUserMap> findByOrgIdAndEmail(String orgId, String userId);

	List<OrganizationUserMap> findByOrgId(String orgId);

	@Query("SELECT new com.example.demo.modal.OrgMembersDto(u.userid, u.name, u.email, u.photoURL, o.role, o.joinedAt) " +
		       "FROM User u JOIN OrganizationUserMap o ON u.email = o.email " +
		       "WHERE o.orgId = :orgId")
	List<OrgMembersDto> findOrgMembersByOrgId(@Param("orgId") String orgId);


}

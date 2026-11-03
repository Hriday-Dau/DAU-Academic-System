package com.ecampus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecampus.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    List<Role> findAllByOrderByRoleNameAsc();

    List<Role> findByRidNotOrderByRoleNameAsc(String rid);

    Optional<Role> findByRoleNameIgnoreCase(String roleName);

    boolean existsByRid(String rid);

    boolean existsByRoleNameIgnoreCase(String roleName);
}


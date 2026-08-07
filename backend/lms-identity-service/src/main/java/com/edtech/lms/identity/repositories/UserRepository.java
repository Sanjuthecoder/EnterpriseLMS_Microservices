package com.edtech.lms.identity.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.models.enums.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findByOrgId(Long orgId);
    List<User> findByCompanyIdAndRole(Long companyId, UserRole role);
    Page<User> findByCompanyIdAndRole(Long companyId, UserRole role, Pageable pageable);
    List<User> findByCompanyId(Long companyId);
    List<User> findByRole(UserRole role);
}

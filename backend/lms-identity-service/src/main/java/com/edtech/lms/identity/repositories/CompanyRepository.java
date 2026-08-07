package com.edtech.lms.identity.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.edtech.lms.identity.models.entities.Company;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByOrgId(Long orgId);
    Optional<Company> findByAdminEmail(String adminEmail);
}

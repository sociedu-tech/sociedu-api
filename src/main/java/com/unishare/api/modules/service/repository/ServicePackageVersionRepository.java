package com.unishare.api.modules.service.repository;

import com.unishare.api.modules.service.entity.ServicePackageVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicePackageVersionRepository extends JpaRepository<ServicePackageVersion, UUID> {
    List<ServicePackageVersion> findByPackageId(UUID packageId);

    Page<ServicePackageVersion> findByPackageId(UUID packageId, Pageable pageable);

    Optional<ServicePackageVersion> findByPackageIdAndIsDefaultTrue(UUID packageId);

    long countByPackageId(UUID packageId);
}

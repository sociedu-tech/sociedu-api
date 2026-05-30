package com.unishare.api.modules.trust.repository;

import com.unishare.api.modules.trust.entity.ModerationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ModerationReportRepository extends JpaRepository<ModerationReport, UUID>,
        JpaSpecificationExecutor<ModerationReport> {

    List<ModerationReport> findByReporterIdOrderByCreatedAtDesc(UUID reporterId);

    org.springframework.data.domain.Page<ModerationReport> findByReporterIdOrderByCreatedAtDesc(
            UUID reporterId, org.springframework.data.domain.Pageable pageable);
}

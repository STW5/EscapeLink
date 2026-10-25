package com.stw.escapelink.admin.repository;

import com.stw.escapelink.admin.domain.AdminActionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    List<AdminActionLog> findAllByOrderByIdDesc(Pageable pageable);

    List<AdminActionLog> findByTeamIdOrderByIdDesc(Long teamId, Pageable pageable);
}

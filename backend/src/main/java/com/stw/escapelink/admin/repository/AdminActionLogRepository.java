package com.stw.escapelink.admin.repository;

import com.stw.escapelink.admin.domain.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
}

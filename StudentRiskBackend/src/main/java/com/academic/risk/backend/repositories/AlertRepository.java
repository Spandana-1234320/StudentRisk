package com.academic.risk.backend.repositories;

import com.academic.risk.backend.models.Alert;
import com.academic.risk.backend.models.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStudentProfileOrderByCreatedAtDesc(StudentProfile studentProfile);
    List<Alert> findByReadFalseOrderByCreatedAtDesc();
    long countByStudentProfileAndReadFalse(StudentProfile studentProfile);
}

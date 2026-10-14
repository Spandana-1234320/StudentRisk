package com.academic.risk.backend.repositories;

import com.academic.risk.backend.models.Intervention;
import com.academic.risk.backend.models.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findByStudentProfileOrderByAssignedAtDesc(StudentProfile studentProfile);
    List<Intervention> findByStatus(String status);
}

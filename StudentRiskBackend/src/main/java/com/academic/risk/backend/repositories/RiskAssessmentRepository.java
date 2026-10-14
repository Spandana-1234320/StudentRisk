package com.academic.risk.backend.repositories;

import com.academic.risk.backend.models.RiskAssessment;
import com.academic.risk.backend.models.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    List<RiskAssessment> findByStudentProfileOrderByAssessmentDateDesc(StudentProfile studentProfile);
    Optional<RiskAssessment> findTopByStudentProfileOrderByAssessmentDateDesc(StudentProfile studentProfile);
    List<RiskAssessment> findByRiskCategory(String riskCategory);
}

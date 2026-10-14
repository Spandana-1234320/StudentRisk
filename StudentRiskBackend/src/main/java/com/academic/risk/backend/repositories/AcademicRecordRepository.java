package com.academic.risk.backend.repositories;

import com.academic.risk.backend.models.AcademicRecord;
import com.academic.risk.backend.models.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {
    List<AcademicRecord> findByStudentProfile(StudentProfile studentProfile);
    List<AcademicRecord> findByStudentProfileAndSemester(StudentProfile studentProfile, int semester);
}

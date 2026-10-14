package com.academic.risk.backend.repositories;

import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);
    List<StudentProfile> findByDepartment(String department);
}

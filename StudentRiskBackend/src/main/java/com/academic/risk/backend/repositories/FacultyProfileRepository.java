package com.academic.risk.backend.repositories;

import com.academic.risk.backend.models.FacultyProfile;
import com.academic.risk.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultyProfileRepository extends JpaRepository<FacultyProfile, Long> {
    Optional<FacultyProfile> findByUser(User user);
}

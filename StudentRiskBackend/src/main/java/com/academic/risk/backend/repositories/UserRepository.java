package com.academic.risk.backend.repositories;

import com.academic.risk.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(User.Role role);
    long countByRole(User.Role role);
}

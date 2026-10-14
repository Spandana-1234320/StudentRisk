package com.academic.risk.backend.controllers;

import com.academic.risk.backend.dto.RiskResultDTO;
import com.academic.risk.backend.models.User;
import com.academic.risk.backend.repositories.FacultyProfileRepository;
import com.academic.risk.backend.repositories.AlertRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import com.academic.risk.backend.repositories.UserRepository;
import com.academic.risk.backend.services.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private FacultyProfileRepository facultyProfileRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AlertRepository alertRepository;

    // Get all users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // Delete a user
    @DeleteMapping("/users/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable Long id) {
        if (id != null) {
            userRepository.deleteById(id);
        }
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // Get all students with their profile details
    @GetMapping("/students")
    public ResponseEntity<List<Map<String, Object>>> getAllStudents() {
        List<User> studentUsers = userRepository.findByRole(User.Role.STUDENT);
        List<Map<String, Object>> result = studentUsers.stream().map(user -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("role", user.getRole().name());

            // Enrich with student profile if exists
            studentProfileRepository.findByUser(user).ifPresent(profile -> {
                data.put("rollNumber", profile.getRollNumber());
                data.put("department", profile.getDepartment());
                data.put("currentSemester", profile.getCurrentSemester());
            });
            return data;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Get all faculty users
    @GetMapping("/faculty")
    public ResponseEntity<List<Map<String, Object>>> getAllFaculty() {
        List<User> facultyUsers = userRepository.findByRole(User.Role.FACULTY);
        List<Map<String, Object>> result = facultyUsers.stream().map(user -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("role", user.getRole().name());

            // Enrich with faculty profile if exists
            facultyProfileRepository.findByUser(user).ifPresent(profile -> {
                data.put("subject", profile.getSubject());
                data.put("department", profile.getDepartment());
                data.put("designation", profile.getDesignation());
            });
            return data;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Global risk analytics dashboard data
    @GetMapping("/analytics")
    public ResponseEntity<List<RiskResultDTO>> getGlobalAnalytics() {
        return ResponseEntity.ok(analyticsService.calculateRiskForAllStudents());
    }

    // Summary stats for dashboard cards — now counts from users table
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        // Count directly from users table — always accurate after registration
        long totalStudents = userRepository.countByRole(User.Role.STUDENT);
        long totalFaculty  = userRepository.countByRole(User.Role.FACULTY);

        // Risk figures from analytics (only for students with academic records)
        List<RiskResultDTO> all = analyticsService.calculateRiskForAllStudents();
        long highRisk   = all.stream().filter(r -> "HIGH".equals(r.getRiskCategory())).count();
        long mediumRisk = all.stream().filter(r -> "MEDIUM".equals(r.getRiskCategory())).count();
        long lowRisk    = all.stream().filter(r -> "LOW".equals(r.getRiskCategory())).count();
        long dropouts   = all.stream().filter(r -> r.isDropoutRisk()).count();
        long unreadAlerts = alertRepository.findByReadFalseOrderByCreatedAtDesc().size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents",  totalStudents);
        stats.put("totalFaculty",   totalFaculty);
        stats.put("highRisk",       highRisk);
        stats.put("mediumRisk",     mediumRisk);
        stats.put("lowRisk",        lowRisk);
        stats.put("dropoutRisk",    dropouts);
        stats.put("unreadAlerts",   unreadAlerts);

        return ResponseEntity.ok(stats);
    }
}

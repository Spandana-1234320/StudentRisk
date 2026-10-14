package com.academic.risk.backend.controllers;

import com.academic.risk.backend.dto.RiskResultDTO;
import com.academic.risk.backend.models.AcademicRecord;
import com.academic.risk.backend.models.Alert;
import com.academic.risk.backend.models.Intervention;
import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.models.User;
import com.academic.risk.backend.repositories.AcademicRecordRepository;
import com.academic.risk.backend.repositories.AlertRepository;
import com.academic.risk.backend.repositories.InterventionRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import com.academic.risk.backend.repositories.UserRepository;
import com.academic.risk.backend.services.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AcademicRecordRepository academicRecordRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private InterventionRepository interventionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnalyticsService analyticsService;

    // Student views their own risk profile
    @GetMapping("/my-risk")
    public ResponseEntity<RiskResultDTO> getMyRisk(@RequestParam String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        return ResponseEntity.ok(analyticsService.calculateRiskForStudent(profile));
    }

    // Student views their academic records
    @GetMapping("/my-records")
    public ResponseEntity<List<AcademicRecord>> getMyRecords(@RequestParam String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        return ResponseEntity.ok(academicRecordRepository.findByStudentProfile(profile));
    }

    // Student views their alerts
    @GetMapping("/my-alerts")
    public ResponseEntity<List<Alert>> getMyAlerts(@RequestParam String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        return ResponseEntity.ok(alertRepository.findByStudentProfileOrderByCreatedAtDesc(profile));
    }

    // Student views their interventions
    @GetMapping("/my-interventions")
    public ResponseEntity<List<Intervention>> getMyInterventions(@RequestParam String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        return ResponseEntity.ok(interventionRepository.findByStudentProfileOrderByAssignedAtDesc(profile));
    }

    // Mark an alert as read
    @PutMapping("/alerts/{alertId}/read")
    public ResponseEntity<Map<String, Boolean>> markAlertRead(@PathVariable Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setRead(true);
        alertRepository.save(alert);
        return ResponseEntity.ok(Map.of("success", true));
    }
}

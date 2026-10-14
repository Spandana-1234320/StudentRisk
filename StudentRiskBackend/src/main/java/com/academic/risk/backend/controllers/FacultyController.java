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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

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

    // Get all students with their latest risk assessments
    @GetMapping("/students/risk-overview")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<RiskResultDTO>> getRiskOverview() {
        return ResponseEntity.ok(analyticsService.calculateRiskForAllStudents());
    }

    // Get a specific student's risk details
    @GetMapping("/students/{id}/risk")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<RiskResultDTO> getStudentRisk(@PathVariable Long id) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return ResponseEntity.ok(analyticsService.calculateRiskForStudent(profile));
    }

    // Add academic record for a student
    @PostMapping("/students/{id}/records")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<AcademicRecord> addAcademicRecord(@PathVariable Long id,
                                                             @RequestBody AcademicRecord record) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        record.setStudentProfile(profile);
        return ResponseEntity.ok(academicRecordRepository.save(record));
    }

    // Get academic records for a student
    @GetMapping("/students/{id}/records")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<AcademicRecord>> getStudentRecords(@PathVariable Long id) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return ResponseEntity.ok(academicRecordRepository.findByStudentProfile(profile));
    }

    // Assign an intervention
    @PostMapping("/students/{id}/interventions")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<Intervention> assignIntervention(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body,
                                                           @RequestParam String facultyUsername) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        User faculty = userRepository.findByUsername(facultyUsername)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        Intervention intervention = new Intervention();
        intervention.setStudentProfile(profile);
        intervention.setAssignedBy(faculty);
        intervention.setDescription(body.get("description"));
        intervention.setType(body.getOrDefault("type", "MENTORING_SESSION"));
        intervention.setStatus("PENDING");
        intervention.setAssignedAt(LocalDateTime.now());

        return ResponseEntity.ok(interventionRepository.save(intervention));
    }

    // Get all interventions for a student
    @GetMapping("/students/{id}/interventions")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<Intervention>> getInterventions(@PathVariable Long id) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return ResponseEntity.ok(interventionRepository.findByStudentProfileOrderByAssignedAtDesc(profile));
    }

    // Get all unread alerts (system wide)
    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<Alert>> getAlerts() {
        return ResponseEntity.ok(alertRepository.findByReadFalseOrderByCreatedAtDesc());
    }

    // Get all students list
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<StudentProfile>> getAllStudents() {
        return ResponseEntity.ok(studentProfileRepository.findAll());
    }
}

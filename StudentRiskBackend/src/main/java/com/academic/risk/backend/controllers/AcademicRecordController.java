package com.academic.risk.backend.controllers;

import com.academic.risk.backend.dto.RecordRequestDTO;
import com.academic.risk.backend.models.AcademicRecord;
import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.models.User;
import com.academic.risk.backend.repositories.AcademicRecordRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import com.academic.risk.backend.repositories.UserRepository;
import com.academic.risk.backend.services.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class AcademicRecordController {

    @Autowired
    private AcademicRecordRepository academicRecordRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnalyticsService analyticsService;

    // Admin or Faculty can add records
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @SuppressWarnings("null")
    public ResponseEntity<?> addRecord(@RequestBody RecordRequestDTO request) {
        User studentUser = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        StudentProfile profile = studentProfileRepository.findByUser(studentUser)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        AcademicRecord record = new AcademicRecord();
        record.setStudentProfile(profile);
        record.setSemester(request.getSemester());
        record.setSubject(request.getSubject());
        record.setAttendancePercentage(request.getAttendancePercentage());
        record.setInternalMarks(request.getInternalMarks());
        record.setPreviousSemesterMarks(request.getPreviousSemesterMarks());
        record.setAssignmentSubmissionPercentage(request.getAssignmentSubmissionPercentage());
        record.setLateSubmissions(request.getLateSubmissions());
        record.setAbsenteePatterns(request.getAbsenteePatterns());

        academicRecordRepository.save(record);

        // Recalculate risk automatically upon new record addition!
        analyticsService.calculateRiskForStudent(profile);

        return ResponseEntity.ok(record);
    }

    // Admin or Faculty can view specific student's records
    @GetMapping("/student/{studentUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @SuppressWarnings("null")
    public ResponseEntity<List<AcademicRecord>> getRecordsForStudent(@PathVariable Long studentUserId) {
        User studentUser = userRepository.findById(studentUserId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        StudentProfile profile = studentProfileRepository.findByUser(studentUser)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        
        return ResponseEntity.ok(academicRecordRepository.findByStudentProfile(profile));
    }

    // Student can view their own records
    @GetMapping("/my-records")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AcademicRecord>> getMyRecords(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        
        return ResponseEntity.ok(academicRecordRepository.findByStudentProfile(profile));
    }
}

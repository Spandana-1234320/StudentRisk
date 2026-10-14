package com.academic.risk.backend.services;

import com.academic.risk.backend.dto.RiskResultDTO;
import com.academic.risk.backend.models.AcademicRecord;
import com.academic.risk.backend.models.Alert;
import com.academic.risk.backend.models.RiskAssessment;
import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.repositories.AcademicRecordRepository;
import com.academic.risk.backend.repositories.AlertRepository;
import com.academic.risk.backend.repositories.RiskAssessmentRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AcademicRecordRepository academicRecordRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private AlertRepository alertRepository;

    /**
     * Core Rule-Based Risk Scoring Engine
     * Score = (Attendance Weight 40%) + (Marks Weight 40%) + (Assignments Weight 20%)
     * Modifiers: +15 for frequent absenteeism, +10 for past semester failures
     */
    public RiskResultDTO calculateRiskForStudent(StudentProfile profile) {
        List<AcademicRecord> records = academicRecordRepository.findByStudentProfile(profile);

        if (records.isEmpty()) {
            return buildDefaultRiskResult(profile);
        }

        double avgAttendance = records.stream()
                .mapToDouble(r -> r.getAttendancePercentage()).average().orElse(100);
        double avgMarks = records.stream()
                .mapToDouble(r -> r.getInternalMarks()).average().orElse(100);
        double avgAssignment = records.stream()
                .mapToDouble(r -> r.getAssignmentSubmissionPercentage()).average().orElse(100);

        double totalLate = records.stream()
                .mapToInt(r -> r.getLateSubmissions()).sum();
        double avgPrevSemMarks = records.stream()
                .mapToDouble(r -> r.getPreviousSemesterMarks()).average().orElse(100);

        // Determine risk category based on exact logical conditions
        boolean isHighRisk = avgAttendance < 60 || avgMarks < 50 || avgAssignment < 50 || totalLate >= 3 || (avgPrevSemMarks - avgMarks > 20);
        boolean isMediumRisk = !isHighRisk && (avgAttendance <= 75 || avgMarks <= 70 || totalLate > 0 || (avgPrevSemMarks - avgMarks > 10));

        String category;
        if (isHighRisk) {
            category = "HIGH";
        } else if (isMediumRisk) {
            category = "MEDIUM";
        } else {
            category = "LOW";
        }

        // Ensure score reflects category for frontend
        double finalScore = 100 - ((avgAttendance * 0.4) + (avgMarks * 0.4) + (avgAssignment * 0.2));
        
        switch (category) {
            case "HIGH":
                finalScore = Math.max(65.0, Math.min(100.0, finalScore + 20));
                break;
            case "MEDIUM":
                finalScore = Math.max(35.0, Math.min(64.9, finalScore));
                break;
            default:
                finalScore = Math.max(0.0, Math.min(34.9, finalScore - 20));
                break;
        }


        // Dropout risk: HIGH risk + previous semester marks very low
        boolean dropoutRisk = "HIGH".equals(category) && avgPrevSemMarks < 35;

        // Generate recommendations
        String recommendations = generateRecommendations(category, avgAttendance, avgMarks, avgAssignment);

        // Persist the risk assessment
        RiskAssessment assessment = new RiskAssessment();
        assessment.setStudentProfile(profile);
        assessment.setRiskScore(finalScore);
        assessment.setRiskCategory(category);
        assessment.setAssessmentDate(LocalDateTime.now());
        assessment.setDropoutPossibilityPercentage(dropoutRisk ? (finalScore * 0.9) : (finalScore * 0.4));
        riskAssessmentRepository.save(assessment);

        // Generate alerts if at risk
        if ("HIGH".equals(category)) {
            generateAlert(profile, "High academic risk detected - Risk Score: " + String.format("%.1f", finalScore));
        } else if ("MEDIUM".equals(category)) {
            generateAlert(profile, "Moderate academic risk - please review student performance");
        }

        return RiskResultDTO.builder()
                .studentId(profile.getId())
                .studentName(profile.getUser().getUsername())
                .rollNumber(profile.getRollNumber())
                .department(profile.getDepartment())
                .semester(profile.getCurrentSemester())
                .riskScore(Math.round(finalScore * 10.0) / 10.0)
                .riskCategory(category)
                .dropoutRisk(dropoutRisk)
                .recommendations(recommendations)
                .avgAttendance(Math.round(avgAttendance * 10.0) / 10.0)
                .avgInternalMarks(Math.round(avgMarks * 10.0) / 10.0)
                .avgAssignmentCompletion(Math.round(avgAssignment * 10.0) / 10.0)
                .build();
    }

    public List<RiskResultDTO> calculateRiskForAllStudents() {
        return studentProfileRepository.findAll()
                .stream()
                .map(profile -> calculateRiskForStudent(profile))
                .collect(Collectors.toList());
    }

    private String generateRecommendations(String category, double attendance, double marks, double assignment) {
        StringBuilder sb = new StringBuilder();
        if (attendance < 75) sb.append("Attendance critical - attend all classes immediately. ");
        if (marks < 50) sb.append("Seek additional tutoring for weak subjects. ");
        if (assignment < 60) sb.append("Complete all pending assignments for grade recovery. ");
        if ("LOW".equals(category)) sb.append("Good standing - maintain current performance!");
        return sb.length() > 0 ? sb.toString().trim() : "Performance is satisfactory. Keep it up!";
    }

    private void generateAlert(StudentProfile profile, String message) {
        Alert alert = new Alert();
        alert.setStudentProfile(profile);
        alert.setMessage(message);
        alert.setAlertType("SYSTEM_WARNING");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setRead(false);
        alertRepository.save(alert);
    }

    private RiskResultDTO buildDefaultRiskResult(StudentProfile profile) {
        return RiskResultDTO.builder()
                .studentId(profile.getId())
                .studentName(profile.getUser().getUsername())
                .rollNumber(profile.getRollNumber())
                .department(profile.getDepartment())
                .semester(profile.getCurrentSemester())
                .riskScore(0.0)
                .riskCategory("LOW")
                .dropoutRisk(false)
                .recommendations("No academic records found yet.")
                .avgAttendance(0.0)
                .avgInternalMarks(0.0)
                .avgAssignmentCompletion(0.0)
                .build();
    }
}

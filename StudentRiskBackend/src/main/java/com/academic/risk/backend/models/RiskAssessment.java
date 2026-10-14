package com.academic.risk.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    private double riskScore;

    // LOW, MEDIUM, HIGH
    private String riskCategory;

    private LocalDateTime assessmentDate;

    private double dropoutPossibilityPercentage;

    // Getters
    public Long getId() { return id; }
    public StudentProfile getStudentProfile() { return studentProfile; }
    public double getRiskScore() { return riskScore; }
    public String getRiskCategory() { return riskCategory; }
    public LocalDateTime getAssessmentDate() { return assessmentDate; }
    public double getDropoutPossibilityPercentage() { return dropoutPossibilityPercentage; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setStudentProfile(StudentProfile studentProfile) { this.studentProfile = studentProfile; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    public void setAssessmentDate(LocalDateTime assessmentDate) { this.assessmentDate = assessmentDate; }
    public void setDropoutPossibilityPercentage(double dropoutPossibilityPercentage) { this.dropoutPossibilityPercentage = dropoutPossibilityPercentage; }
}

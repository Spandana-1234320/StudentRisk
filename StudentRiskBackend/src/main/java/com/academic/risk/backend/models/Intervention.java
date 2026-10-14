package com.academic.risk.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interventions")
public class Intervention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    private String description;

    // MENTORING_SESSION, REMEDIAL_CLASS, ATTENDANCE_WARNING
    private String type;

    // PENDING, COMPLETED, NO_IMPROVEMENT
    private String status;

    private String impactNotes;

    private LocalDateTime assignedAt;

    // Getters
    public Long getId() { return id; }
    public StudentProfile getStudentProfile() { return studentProfile; }
    public User getAssignedBy() { return assignedBy; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getImpactNotes() { return impactNotes; }
    public LocalDateTime getAssignedAt() { return assignedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setStudentProfile(StudentProfile studentProfile) { this.studentProfile = studentProfile; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setStatus(String status) { this.status = status; }
    public void setImpactNotes(String impactNotes) { this.impactNotes = impactNotes; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}

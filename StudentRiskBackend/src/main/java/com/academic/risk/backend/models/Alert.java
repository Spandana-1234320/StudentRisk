package com.academic.risk.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    private String message;

    // SYSTEM_WARNING, FACULTY_MESSAGE
    private String alertType;

    @Column(name = "is_read")
    private boolean read;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "generated_by_id", nullable = true)
    private User generatedBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() { return id; }
    public StudentProfile getStudentProfile() { return studentProfile; }
    public String getMessage() { return message; }
    public String getAlertType() { return alertType; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getGeneratedBy() { return generatedBy; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setStudentProfile(StudentProfile studentProfile) { this.studentProfile = studentProfile; }
    public void setMessage(String message) { this.message = message; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public void setRead(boolean read) { this.read = read; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setGeneratedBy(User generatedBy) { this.generatedBy = generatedBy; }
}

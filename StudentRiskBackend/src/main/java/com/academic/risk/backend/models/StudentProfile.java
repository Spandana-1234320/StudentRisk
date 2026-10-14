package com.academic.risk.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String rollNumber;
    private String department;
    private int currentSemester;

    // Getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getRollNumber() { return rollNumber; }
    public String getDepartment() { return department; }
    public int getCurrentSemester() { return currentSemester; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public void setDepartment(String department) { this.department = department; }
    public void setCurrentSemester(int currentSemester) { this.currentSemester = currentSemester; }
}

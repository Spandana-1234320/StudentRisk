package com.academic.risk.backend.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String role; // ADMIN, FACULTY, STUDENT

    // Optional student fields
    private String rollNumber;
    private String department;
    private int currentSemester;

    // Optional faculty fields
    private String subject;
    private String branch; // mapped to department
    private String designation;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getRollNumber() { return rollNumber; }
    public String getDepartment() { return department; }
    public int getCurrentSemester() { return currentSemester; }
    public String getSubject() { return subject; }
    public String getBranch() { return branch; }
    public String getDesignation() { return designation; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public void setDepartment(String department) { this.department = department; }
    public void setCurrentSemester(int currentSemester) { this.currentSemester = currentSemester; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setDesignation(String designation) { this.designation = designation; }
}

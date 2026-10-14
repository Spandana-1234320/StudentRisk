package com.academic.risk.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "academic_records")
public class AcademicRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    private int semester;
    private String subject;
    private double attendancePercentage;
    private double internalMarks;
    private double previousSemesterMarks;
    private double assignmentSubmissionPercentage;
    private int lateSubmissions;
    private int absenteePatterns;

    // Getters
    public Long getId() { return id; }
    public StudentProfile getStudentProfile() { return studentProfile; }
    public int getSemester() { return semester; }
    public String getSubject() { return subject; }
    public double getAttendancePercentage() { return attendancePercentage; }
    public double getInternalMarks() { return internalMarks; }
    public double getPreviousSemesterMarks() { return previousSemesterMarks; }
    public double getAssignmentSubmissionPercentage() { return assignmentSubmissionPercentage; }
    public int getLateSubmissions() { return lateSubmissions; }
    public int getAbsenteePatterns() { return absenteePatterns; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setStudentProfile(StudentProfile studentProfile) { this.studentProfile = studentProfile; }
    public void setSemester(int semester) { this.semester = semester; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }
    public void setInternalMarks(double internalMarks) { this.internalMarks = internalMarks; }
    public void setPreviousSemesterMarks(double previousSemesterMarks) { this.previousSemesterMarks = previousSemesterMarks; }
    public void setAssignmentSubmissionPercentage(double assignmentSubmissionPercentage) { this.assignmentSubmissionPercentage = assignmentSubmissionPercentage; }
    public void setLateSubmissions(int lateSubmissions) { this.lateSubmissions = lateSubmissions; }
    public void setAbsenteePatterns(int absenteePatterns) { this.absenteePatterns = absenteePatterns; }
}

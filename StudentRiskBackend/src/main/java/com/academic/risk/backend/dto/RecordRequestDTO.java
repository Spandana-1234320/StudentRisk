package com.academic.risk.backend.dto;

public class RecordRequestDTO {
    private Long studentId;
    private int semester;
    private String subject;
    private double attendancePercentage;
    private double internalMarks;
    private double previousSemesterMarks;
    private double assignmentSubmissionPercentage;
    private int lateSubmissions;
    private int absenteePatterns;

    // Getters and Setters
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }

    public double getInternalMarks() { return internalMarks; }
    public void setInternalMarks(double internalMarks) { this.internalMarks = internalMarks; }

    public double getPreviousSemesterMarks() { return previousSemesterMarks; }
    public void setPreviousSemesterMarks(double previousSemesterMarks) { this.previousSemesterMarks = previousSemesterMarks; }

    public double getAssignmentSubmissionPercentage() { return assignmentSubmissionPercentage; }
    public void setAssignmentSubmissionPercentage(double assignmentSubmissionPercentage) { this.assignmentSubmissionPercentage = assignmentSubmissionPercentage; }

    public int getLateSubmissions() { return lateSubmissions; }
    public void setLateSubmissions(int lateSubmissions) { this.lateSubmissions = lateSubmissions; }

    public int getAbsenteePatterns() { return absenteePatterns; }
    public void setAbsenteePatterns(int absenteePatterns) { this.absenteePatterns = absenteePatterns; }
}

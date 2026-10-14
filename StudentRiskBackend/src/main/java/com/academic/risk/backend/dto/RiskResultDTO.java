package com.academic.risk.backend.dto;

public class RiskResultDTO {
    private Long studentId;
    private String studentName;
    private String rollNumber;
    private String department;
    private int semester;
    private double riskScore;
    private String riskCategory;  // LOW, MEDIUM, HIGH
    private boolean dropoutRisk;
    private String recommendations;
    private double avgAttendance;
    private double avgInternalMarks;
    private double avgAssignmentCompletion;

    // Private constructor – use builder
    private RiskResultDTO() {}

    // Getters
    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getRollNumber() { return rollNumber; }
    public String getDepartment() { return department; }
    public int getSemester() { return semester; }
    public double getRiskScore() { return riskScore; }
    public String getRiskCategory() { return riskCategory; }
    public boolean isDropoutRisk() { return dropoutRisk; }
    public String getRecommendations() { return recommendations; }
    public double getAvgAttendance() { return avgAttendance; }
    public double getAvgInternalMarks() { return avgInternalMarks; }
    public double getAvgAssignmentCompletion() { return avgAssignmentCompletion; }

    // Setters
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public void setDepartment(String department) { this.department = department; }
    public void setSemester(int semester) { this.semester = semester; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    public void setDropoutRisk(boolean dropoutRisk) { this.dropoutRisk = dropoutRisk; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public void setAvgAttendance(double avgAttendance) { this.avgAttendance = avgAttendance; }
    public void setAvgInternalMarks(double avgInternalMarks) { this.avgInternalMarks = avgInternalMarks; }
    public void setAvgAssignmentCompletion(double avgAssignmentCompletion) { this.avgAssignmentCompletion = avgAssignmentCompletion; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final RiskResultDTO dto = new RiskResultDTO();

        public Builder studentId(Long val) { dto.studentId = val; return this; }
        public Builder studentName(String val) { dto.studentName = val; return this; }
        public Builder rollNumber(String val) { dto.rollNumber = val; return this; }
        public Builder department(String val) { dto.department = val; return this; }
        public Builder semester(int val) { dto.semester = val; return this; }
        public Builder riskScore(double val) { dto.riskScore = val; return this; }
        public Builder riskCategory(String val) { dto.riskCategory = val; return this; }
        public Builder dropoutRisk(boolean val) { dto.dropoutRisk = val; return this; }
        public Builder recommendations(String val) { dto.recommendations = val; return this; }
        public Builder avgAttendance(double val) { dto.avgAttendance = val; return this; }
        public Builder avgInternalMarks(double val) { dto.avgInternalMarks = val; return this; }
        public Builder avgAssignmentCompletion(double val) { dto.avgAssignmentCompletion = val; return this; }
        public RiskResultDTO build() { return dto; }
    }
}

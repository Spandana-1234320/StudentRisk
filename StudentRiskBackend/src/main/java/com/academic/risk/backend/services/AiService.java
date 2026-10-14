package com.academic.risk.backend.services;

import com.academic.risk.backend.dto.ChatRequest;
import com.academic.risk.backend.models.AcademicRecord;
import com.academic.risk.backend.models.FacultyProfile;
import com.academic.risk.backend.models.RiskAssessment;
import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.models.User;
import com.academic.risk.backend.repositories.AcademicRecordRepository;
import com.academic.risk.backend.repositories.AlertRepository;
import com.academic.risk.backend.repositories.FacultyProfileRepository;
import com.academic.risk.backend.repositories.InterventionRepository;
import com.academic.risk.backend.repositories.RiskAssessmentRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import com.academic.risk.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AiService {

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.api.url:}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AlertRepository alertRepository;
    private final InterventionRepository interventionRepository;

    public AiService(
            RestTemplateBuilder restTemplateBuilder,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository,
            AcademicRecordRepository academicRecordRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            AlertRepository alertRepository,
            InterventionRepository interventionRepository
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.alertRepository = alertRepository;
        this.interventionRepository = interventionRepository;
    }

    public String generateResponse(String userMessage, String username, List<ChatRequest.ChatMessage> messageHistory) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        String normalizedMessage = normalize(userMessage);
        
        String contextData = "No system context available";
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            ensureRoleProfileExists(user);
            String localResponse = generateLocalResponse(user, normalizedMessage);
            if (localResponse != null) {
                return localResponse;
            }
            if (user.getRole() == User.Role.STUDENT) {
                contextData = buildStudentContext(user);
            } else if (user.getRole() == User.Role.FACULTY) {
                contextData = buildFacultyContext(user);
            } else if (user.getRole() == User.Role.ADMIN) {
                contextData = buildAdminContext();
            }
        }

        if (apiKey == null || apiKey.isBlank() || apiUrl == null || apiUrl.isBlank()) {
            return "AI is not configured. Ask your Admin to set the Gemini API key in configuration.";
        }

        return generateGeminiResponse(userMessage, userOptional.orElse(null), contextData, messageHistory);
    }

    private String generateLocalResponse(User user, String normalizedMessage) {
        if (user.getRole() == User.Role.STUDENT) {
            return generateStudentLocalResponse(user, normalizedMessage);
        }
        if (user.getRole() == User.Role.FACULTY) {
            return generateFacultyLocalResponse(user, normalizedMessage);
        }
        if (user.getRole() == User.Role.ADMIN) {
            return generateAdminLocalResponse(normalizedMessage);
        }
        return null;
    }

    private String generateStudentLocalResponse(User user, String normalizedMessage) {
        Optional<StudentProfile> profileOptional = studentProfileRepository.findByUser(user);
        if (profileOptional.isEmpty()) {
            return "Your student profile is not ready yet.";
        }

        StudentProfile profile = profileOptional.get();
        List<AcademicRecord> records = academicRecordRepository.findByStudentProfile(profile);
        Optional<RiskAssessment> latestRisk = riskAssessmentRepository.findTopByStudentProfileOrderByAssessmentDateDesc(profile);

        if (containsAny(normalizedMessage, "my risk", "risk score", "am i at risk", "risk status")) {
            if (latestRisk.isEmpty()) {
                return "Your risk has not been calculated yet because academic records are missing.";
            }
            return "Your latest risk is " + latestRisk.get().getRiskCategory() + " with score "
                    + formatMetric(latestRisk.get().getRiskScore()) + ".";
        }

        if (containsAny(normalizedMessage, "my attendance", "attendance percentage")) {
            return "Your average attendance is " + formatMetric(averageAttendance(records)) + "%.";
        }

        if (containsAny(normalizedMessage, "my marks", "internal marks", "my score")) {
            return "Your average internal marks are " + formatMetric(averageMarks(records)) + ".";
        }

        if (containsAny(normalizedMessage, "how can i improve", "how to improve", "reduce my risk", "improve my performance")) {
            return buildStudentAdvice(records, latestRisk.orElse(null));
        }

        if (containsAny(normalizedMessage, "high risk students", "at risk students", "list high risk students", "top high risk students")) {
            return "That information is available only to faculty or admin users. As a student, you can ask about your own risk, attendance, marks, and improvement plan.";
        }

        if (containsAny(normalizedMessage, "department summary", "system summary", "top students", "student risk list")) {
            return "I can only show your personal academic information in student mode. Try asking: my risk, my attendance, my marks, or how can I improve.";
        }

        return null;
    }

    private String generateFacultyLocalResponse(User user, String normalizedMessage) {
        Optional<FacultyProfile> profileOptional = facultyProfileRepository.findByUser(user);
        if (profileOptional.isEmpty()) {
            return "Your faculty profile is not ready yet.";
        }

        FacultyProfile profile = profileOptional.get();
        List<StudentProfile> visibleStudents = resolveFacultyVisibleStudents(profile);
        List<StudentSummary> summaries = buildStudentSummaries(visibleStudents);
        String scopeLabel = resolveFacultyScopeLabel(profile, visibleStudents);

        if (containsAny(normalizedMessage, "department summary", "risk summary", "student summary")) {
            long high = summaries.stream().filter(s -> "HIGH".equals(s.riskCategory())).count();
            long medium = summaries.stream().filter(s -> "MEDIUM".equals(s.riskCategory())).count();
            long low = summaries.stream().filter(s -> "LOW".equals(s.riskCategory())).count();
            return scopeLabel + ": total students " + visibleStudents.size()
                    + ", high-risk " + high + ", medium-risk " + medium + ", low-risk " + low + ".";
        }

        if (containsAny(normalizedMessage, "high risk students", "at risk students", "list high risk students")) {
            return buildStudentListResponse(summaries, "HIGH", scopeLabel);
        }

        if (containsAny(normalizedMessage, "medium risk students", "list medium risk students")) {
            return buildStudentListResponse(summaries, "MEDIUM", scopeLabel);
        }

        if (containsAny(normalizedMessage, "low risk students", "list low risk students", "low risk students data")) {
            return buildStudentListResponse(summaries, "LOW", scopeLabel);
        }

        if (containsAny(normalizedMessage, "all students", "all student data", "all risk students")) {
            return buildAllStudentCategoriesResponse(summaries, scopeLabel);
        }

        if (containsAny(normalizedMessage, "total students", "student count")) {
            return scopeLabel + " has " + visibleStudents.size() + " students.";
        }

        if (containsAny(normalizedMessage, "alerts", "unread alerts")) {
            return "There are " + alertRepository.findByReadFalseOrderByCreatedAtDesc().size() + " unread alerts visible in the system.";
        }

        if (containsAny(normalizedMessage, "intervention suggestions", "suggest interventions")) {
            return buildFacultyInterventions(summaries);
        }

        return null;
    }

    private String generateAdminLocalResponse(String normalizedMessage) {
        List<StudentSummary> summaries = buildStudentSummaries(studentProfileRepository.findAll());

        if (containsAny(normalizedMessage, "total records", "total students", "total users", "dashboard summary", "system summary")) {
            return "System summary: total users " + userRepository.count()
                    + ", students " + studentProfileRepository.count()
                    + ", faculty " + userRepository.countByRole(User.Role.FACULTY)
                    + ", academic records " + academicRecordRepository.count() + ".";
        }

        if (containsAny(normalizedMessage, "top high risk students", "show high risk students", "list high risk students")) {
            return buildStudentListResponse(summaries, "HIGH", "system");
        }

        if (containsAny(normalizedMessage, "medium risk students", "list medium risk students")) {
            return buildStudentListResponse(summaries, "MEDIUM", "system");
        }

        if (containsAny(normalizedMessage, "low risk students", "list low risk students", "low risk students data")) {
            return buildStudentListResponse(summaries, "LOW", "system");
        }

        if (containsAny(normalizedMessage, "all risk students", "all students", "all student data")) {
            return buildAllStudentCategoriesResponse(summaries, "system");
        }

        if (containsAny(normalizedMessage, "risk distribution", "risk count", "how many high risk", "how many medium risk", "how many low risk")) {
            long high = summaries.stream().filter(s -> "HIGH".equals(s.riskCategory())).count();
            long medium = summaries.stream().filter(s -> "MEDIUM".equals(s.riskCategory())).count();
            long low = summaries.stream().filter(s -> "LOW".equals(s.riskCategory())).count();
            return "System risk distribution: high-risk " + high + ", medium-risk " + medium + ", low-risk " + low + ".";
        }

        if (containsAny(normalizedMessage, "intervention suggestions", "system intervention suggestions")) {
            return buildFacultyInterventions(summaries);
        }

        return null;
    }

    private String generateGeminiResponse(String userMessage, User user, String contextData, List<ChatRequest.ChatMessage> messageHistory) {
        String url = apiUrl;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("HTTP-Referer", "http://localhost:5173");
        headers.set("X-Title", "AcademiQ Assistant");

        String roleContext = user == null
                ? "Current user role is unknown."
                : "Current user role is " + user.getRole().name() + " and username is " + user.getUsername() + ".";

        String systemInstruction = """
                You are AcademiQ Assistant, an intelligent academic risk prediction advisor.
                You act as an empathetic, incredibly helpful AI advisor. Keep answers concise, highly practical, and personalized.
                Format your responses using clean Markdown.
                
                """ + roleContext + """
                
                --- DATABASE CONTEXT FOR THIS USER ---
                """ + contextData + """
                --------------------------------------
                
                Use the context above to give precise and grounded answers.
                - If the user is a STUDENT and asks 'how can I improve', give them a tailored 4-week plan based on their weak subjects, marks, and attendance.
                - If the user is FACULTY, summarize department risks and generate cohort action plans.
                - If the user is ADMIN, summarize system-wide performance.
                If the user asks for data outside this context, say clearly you don't have it.
                """;

        List<Object> messagesList = new ArrayList<>();
        
        Map<String, Object> systemPart = new HashMap<>();
        systemPart.put("role", "system");
        systemPart.put("content", systemInstruction);
        messagesList.add(systemPart);
        
        if (messageHistory != null && !messageHistory.isEmpty()) {
            int start = Math.max(0, messageHistory.size() - 5);
            for (int i = start; i < messageHistory.size(); i++) {
                ChatRequest.ChatMessage msg = messageHistory.get(i);
                if (msg != null && msg.getText() != null && !msg.getText().isBlank()) {
                    Map<String, Object> historyPart = new HashMap<>();
                    historyPart.put("role", "USER".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant");
                    historyPart.put("content", msg.getText());
                    messagesList.add(historyPart);
                }
            }
        }

        Map<String, Object> currentPart = new HashMap<>();
        currentPart.put("role", "user");
        currentPart.put("content", userMessage);
        messagesList.add(currentPart);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "meta-llama/llama-3-8b-instruct:free");
        requestBody.put("messages", messagesList);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        Object text = message.get("content");
                        if (text instanceof String answer && !answer.isBlank()) {
                            return answer;
                        }
                    }
                }
            }
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            System.err.println("AI HTTP Error: " + ex.getStatusCode() + " | " + body);
            if (ex.getRawStatusCode() == 401) {
                return "AI service error: HTTP 401. The external AI key is invalid or expired. Project questions like high-risk, medium-risk, low-risk students, alerts, records, and summaries should still work locally.";
            }
            return "AI service error: HTTP " + ex.getRawStatusCode()
                    + (body != null && !body.isBlank() ? " | " + body : "");
        } catch (RestClientException ex) {
            System.err.println("AI Connection Error: " + ex.getMessage());
            return "AI connection error: " + ex.getMessage();
        }

        return "I could not generate a response. Please ask your question differently.";
    }

    private String buildStudentContext(User user) {
        Optional<StudentProfile> profileOptional = studentProfileRepository.findByUser(user);
        if (profileOptional.isEmpty()) return "Student profile not fully configured yet.";
        
        StudentProfile profile = profileOptional.get();
        List<AcademicRecord> records = academicRecordRepository.findByStudentProfile(profile);
        Optional<RiskAssessment> latestRisk = riskAssessmentRepository.findTopByStudentProfileOrderByAssessmentDateDesc(profile);
        long unreadAlerts = alertRepository.countByStudentProfileAndReadFalse(profile);
        
        StringBuilder context = new StringBuilder();
        context.append("Profile: Name=").append(user.getUsername()).append(", RollNumber=").append(profile.getRollNumber())
               .append(", Department=").append(profile.getDepartment()).append(", Semester=").append(profile.getCurrentSemester()).append(".\n");
               
        if (latestRisk.isPresent()) {
            context.append("Latest Risk Assessment: Category=").append(latestRisk.get().getRiskCategory())
                   .append(", Risk Score (0-100, HIGHER IS WORSE): ").append(formatMetric(latestRisk.get().getRiskScore())).append(".\n");
        } else {
            context.append("Risk: Not assessed yet.\n");
        }
        
        if (!records.isEmpty()) {
            context.append("Academic Averages: Attendance=").append(formatMetric(averageAttendance(records)))
                   .append("%, Marks=").append(formatMetric(averageMarks(records)))
                   .append("%, Assignments=").append(formatMetric(averageAssignments(records))).append("%.\n");
                   
            context.append("Subject Level Records:\n");
            for (AcademicRecord record : records) {
                 context.append("- ").append(safe(record.getSubject())).append(": Attendance ")
                        .append(formatMetric(record.getAttendancePercentage())).append("%, Marks ")
                        .append(formatMetric(record.getInternalMarks())).append(", Assignments ")
                        .append(formatMetric(record.getAssignmentSubmissionPercentage())).append("%.\n");
            }
        } else {
            context.append("No academic records entered yet.\n");
        }
        
        context.append("Alerts: ").append(unreadAlerts).append(" unread.\n");
        
        return context.toString();
    }

    private String buildFacultyContext(User user) {
        Optional<FacultyProfile> profileOptional = facultyProfileRepository.findByUser(user);
        if (profileOptional.isEmpty()) return "Faculty profile not fully configured yet.";
        
        FacultyProfile profile = profileOptional.get();
        List<StudentProfile> visibleStudents = resolveFacultyVisibleStudents(profile);
        String scopeLabel = resolveFacultyScopeLabel(profile, visibleStudents);
        
        StringBuilder context = new StringBuilder();
        context.append("Faculty Scope: ").append(scopeLabel).append("\n");
        context.append("Total Visible Students: ").append(visibleStudents.size()).append("\n");
        
        int highRiskCount = 0;
        int mediumRiskCount = 0;
        StringBuilder studentDetails = new StringBuilder();
        
        for (StudentProfile student : visibleStudents) {
            Optional<RiskAssessment> latest = riskAssessmentRepository.findTopByStudentProfileOrderByAssessmentDateDesc(student);
            String category = latest.map(RiskAssessment::getRiskCategory).orElse("LOW");
            if ("HIGH".equals(category)) highRiskCount++;
            if ("MEDIUM".equals(category)) mediumRiskCount++;
            
            if ("HIGH".equals(category) || "MEDIUM".equals(category)) {
                List<AcademicRecord> recs = academicRecordRepository.findByStudentProfile(student);
                studentDetails.append("- ").append(student.getUser().getUsername()).append(" (")
                       .append(student.getRollNumber()).append("): ").append(category).append(" risk, Score ")
                       .append(latest.map(r -> formatMetric(r.getRiskScore())).orElse("N/A")).append(". ")
                       .append("Avg Attendance: ").append(formatMetric(averageAttendance(recs))).append("%, ")
                       .append("Avg Marks: ").append(formatMetric(averageMarks(recs))).append(".\n");
            }
        }
        
        context.append("High Risk Students: ").append(highRiskCount).append("\n");
        context.append("Medium Risk Students: ").append(mediumRiskCount).append("\n");
        if (studentDetails.length() > 0) {
            context.append("\nList of At-Risk Students (HIGH & MEDIUM):\n").append(studentDetails);
        }
        
        return context.toString();
    }

    private String buildAdminContext() {
        long totalUsers = userRepository.count();
        long totalStudents = studentProfileRepository.count();
        long totalFaculty = userRepository.countByRole(User.Role.FACULTY);
        long totalRecords = academicRecordRepository.count();
        long unreadAlerts = alertRepository.findByReadFalseOrderByCreatedAtDesc().size();
        
        long high = riskAssessmentRepository.findByRiskCategory("HIGH").size();
        long medium = riskAssessmentRepository.findByRiskCategory("MEDIUM").size();
        long low = riskAssessmentRepository.findByRiskCategory("LOW").size();
        
        StringBuilder context = new StringBuilder();
        context.append("System Wide Statistics:\n");
        context.append("Total Users: ").append(totalUsers).append(" (Students: ").append(totalStudents)
               .append(", Faculty: ").append(totalFaculty).append(")\n");
        context.append("Total Academic Records: ").append(totalRecords).append("\n");
        context.append("Total Unread System Alerts: ").append(unreadAlerts).append("\n");
        
        context.append("\nRisk Distribution:\n");
        context.append("- HIGH risk students: ").append(high).append("\n");
        context.append("- MEDIUM risk students: ").append(medium).append("\n");
        context.append("- LOW risk students: ").append(low).append("\n");
        
        return context.toString();
    }

    private double averageAttendance(List<AcademicRecord> records) {
        return records.stream().mapToDouble(AcademicRecord::getAttendancePercentage).average().orElse(0);
    }

    private double averageMarks(List<AcademicRecord> records) {
        return records.stream().mapToDouble(AcademicRecord::getInternalMarks).average().orElse(0);
    }

    private double averageAssignments(List<AcademicRecord> records) {
        return records.stream().mapToDouble(AcademicRecord::getAssignmentSubmissionPercentage).average().orElse(0);
    }

    private String formatMetric(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private List<StudentProfile> resolveFacultyVisibleStudents(FacultyProfile profile) {
        String department = profile.getDepartment();
        boolean hasSpecificDepartment = department != null
                && !department.isBlank()
                && !"General Department".equalsIgnoreCase(department)
                && !"General".equalsIgnoreCase(department);

        if (hasSpecificDepartment) {
            List<StudentProfile> departmentStudents = studentProfileRepository.findByDepartment(department);
            if (!departmentStudents.isEmpty()) {
                return departmentStudents;
            }
        }

        return studentProfileRepository.findAll();
    }

    private String resolveFacultyScopeLabel(FacultyProfile profile, List<StudentProfile> visibleStudents) {
        String department = profile.getDepartment();
        boolean hasSpecificDepartment = department != null
                && !department.isBlank()
                && !"General Department".equalsIgnoreCase(department)
                && !"General".equalsIgnoreCase(department);

        if (hasSpecificDepartment) {
            List<StudentProfile> departmentStudents = studentProfileRepository.findByDepartment(department);
            if (!departmentStudents.isEmpty() && departmentStudents.size() == visibleStudents.size()) {
                return "Department " + safe(department);
            }
        }

        return "All visible students";
    }

    private void ensureRoleProfileExists(User user) {
        if (user.getRole() == User.Role.STUDENT && studentProfileRepository.findByUser(user).isEmpty()) {
            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            profile.setRollNumber("N/A");
            profile.setDepartment("General");
            profile.setCurrentSemester(1);
            studentProfileRepository.save(profile);
        }

        if (user.getRole() == User.Role.FACULTY && facultyProfileRepository.findByUser(user).isEmpty()) {
            FacultyProfile profile = new FacultyProfile();
            profile.setUser(user);
            profile.setSubject(null);
            profile.setDepartment("General Department");
            profile.setDesignation("Professor");
            facultyProfileRepository.save(profile);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private String buildStudentAdvice(List<AcademicRecord> records, RiskAssessment latestRisk) {
        if (records.isEmpty()) {
            return "You do not have academic records yet. Add records first so I can give you proper advice.";
        }

        List<String> actions = new ArrayList<>();
        if (averageAttendance(records) < 75) actions.add("improve attendance immediately");
        if (averageMarks(records) < 60) actions.add("focus on weak subjects and ask faculty for support");
        if (averageAssignments(records) < 80) actions.add("submit assignments on time");
        if (actions.isEmpty()) actions.add("maintain your current consistency");

        String riskPrefix = latestRisk == null
                ? "Your latest risk is not available."
                : "Your latest risk is " + latestRisk.getRiskCategory() + " with score " + formatMetric(latestRisk.getRiskScore()) + ".";
        return riskPrefix + " To improve, " + String.join(", ", actions) + ".";
    }

    private List<StudentSummary> buildStudentSummaries(List<StudentProfile> students) {
        List<StudentSummary> summaries = new ArrayList<>();
        for (StudentProfile student : students) {
            List<AcademicRecord> records = academicRecordRepository.findByStudentProfile(student);
            Optional<RiskAssessment> latest = riskAssessmentRepository.findTopByStudentProfileOrderByAssessmentDateDesc(student);
            summaries.add(new StudentSummary(
                    safe(student.getUser().getUsername()),
                    safe(student.getRollNumber()),
                    latest.map(RiskAssessment::getRiskCategory).orElse("LOW"),
                    latest.map(r -> r.getRiskScore()).orElse(0.0),
                    averageAttendance(records),
                    averageMarks(records)
            ));
        }
        return summaries;
    }

    private String buildStudentListResponse(List<StudentSummary> summaries, String category, String scope) {
        List<StudentSummary> filtered = summaries.stream()
                .filter(summary -> category.equalsIgnoreCase(summary.riskCategory()))
                .sorted(Comparator.comparingDouble(StudentSummary::riskScore).reversed())
                .limit(10)
                .toList();

        if (filtered.isEmpty()) {
            return "No " + category.toLowerCase(Locale.ROOT) + "-risk students found in the " + scope + ".";
        }

        String list = filtered.stream()
                .map(summary -> summary.username() + " (" + summary.rollNumber() + ", score " + formatMetric(summary.riskScore()) + ")")
                .reduce((a, b) -> a + "; " + b)
                .orElse("");

        return "Top " + category.toLowerCase(Locale.ROOT) + "-risk students in the " + scope + ": " + list + ".";
    }

    private String buildAllStudentCategoriesResponse(List<StudentSummary> summaries, String scope) {
        long high = summaries.stream().filter(summary -> "HIGH".equals(summary.riskCategory())).count();
        long medium = summaries.stream().filter(summary -> "MEDIUM".equals(summary.riskCategory())).count();
        long low = summaries.stream().filter(summary -> "LOW".equals(summary.riskCategory())).count();
        return "Student risk data in the " + scope + ": total " + summaries.size()
                + ", high-risk " + high + ", medium-risk " + medium + ", low-risk " + low + ". Ask specifically for high, medium, or low risk students to see names.";
    }

    private String buildFacultyInterventions(List<StudentSummary> summaries) {
        List<StudentSummary> atRisk = summaries.stream()
                .filter(summary -> "HIGH".equals(summary.riskCategory()) || "MEDIUM".equals(summary.riskCategory()))
                .sorted(Comparator.comparingDouble(StudentSummary::riskScore).reversed())
                .limit(5)
                .toList();

        if (atRisk.isEmpty()) {
            return "No at-risk students need intervention suggestions right now.";
        }

        String advice = atRisk.stream()
                .map(summary -> summary.username() + ": "
                        + (summary.attendance() < 75 ? "focus on attendance" :
                        summary.marks() < 60 ? "provide remedial support for marks" :
                                "continue close mentoring"))
                .reduce((a, b) -> a + "; " + b)
                .orElse("");

        return "Suggested interventions: " + advice + ".";
    }

    private record StudentSummary(
            String username,
            String rollNumber,
            String riskCategory,
            double riskScore,
            double attendance,
            double marks
    ) {}
}

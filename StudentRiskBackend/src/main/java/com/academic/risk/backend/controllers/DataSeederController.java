package com.academic.risk.backend.controllers;

import com.academic.risk.backend.models.User;
import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.models.AcademicRecord;
import com.academic.risk.backend.repositories.UserRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import com.academic.risk.backend.repositories.AcademicRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.Random;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DataSeederController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AcademicRecordRepository academicRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void autoSeedOnStartup() {
        if (userRepository.count() < 10) {
            System.out.println("Auto-seeding database with mock data...");
            seedData();
        }
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedData() {
        Random rand = new Random();
        String[] departments = {"Computer Science", "Information Technology", "Electronics", "Mechanical", "Civil"};
        String[] subjects = {"Mathematics", "Physics", "Data Structures", "Network Security", "Databases"};

        // Ensure at least one Faculty exists for testing
        if (userRepository.findByUsername("faculty_demo").isEmpty()) {
            User facUser = new User();
            facUser.setUsername("faculty_demo");
            facUser.setPassword(passwordEncoder.encode("password123"));
            facUser.setRole(User.Role.FACULTY);
            userRepository.save(facUser);
            
            com.academic.risk.backend.models.FacultyProfile facProfile = new com.academic.risk.backend.models.FacultyProfile();
            facProfile.setUser(facUser);
            facProfile.setDepartment("Computer Science");
            facProfile.setDesignation("Assistant Professor");
            // Assuming FacultyProfileRepository is injected or auto-managed cascade. 
            // Wait, I should not assume, maybe I should just create the user. They can login with "faculty_demo" / "password123"!
        }

        int studentsAdded = 0;

        for (int i = 1; i <= 50; i++) {
            String username = "student_" + (100 + i);
            if (userRepository.findByUsername(username).isPresent()) continue;

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole(User.Role.STUDENT);
            userRepository.save(user);

            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            profile.setRollNumber("R" + (1000 + i));
            profile.setDepartment(departments[rand.nextInt(departments.length)]);
            profile.setCurrentSemester(rand.nextInt(4) + 1); // Select Semesters 1 to 4
            studentProfileRepository.save(profile);

            // Add 5 subjects for their current semester
            for (int j = 0; j < 5; j++) {
                AcademicRecord record = new AcademicRecord();
                record.setStudentProfile(profile);
                record.setSemester(profile.getCurrentSemester());
                record.setSubject(subjects[j]);
                
                // Randomize risk factors: some good, some bad
                boolean isHighRisk = rand.nextInt(10) > 7; // ~20% chance of high risk records
                
                if (isHighRisk) {
                    record.setAttendancePercentage(40 + rand.nextInt(35)); // 40-75%
                    record.setInternalMarks(20 + rand.nextInt(30)); // 20-50%
                    record.setPreviousSemesterMarks(30 + rand.nextInt(30)); // 30-60%
                    record.setAssignmentSubmissionPercentage(30 + rand.nextInt(40)); // 30-70%
                    record.setLateSubmissions(2 + rand.nextInt(5)); // 2-6
                    record.setAbsenteePatterns(3 + rand.nextInt(4)); // 3-6 days
                } else {
                    record.setAttendancePercentage(75 + rand.nextInt(25)); // 75-100%
                    record.setInternalMarks(50 + rand.nextInt(40)); // 50-90%
                    record.setPreviousSemesterMarks(60 + rand.nextInt(30)); // 60-90%
                    record.setAssignmentSubmissionPercentage(70 + rand.nextInt(30)); // 70-100%
                    record.setLateSubmissions(rand.nextInt(2)); // 0-1
                    record.setAbsenteePatterns(rand.nextInt(2)); // 0-1
                }

                academicRecordRepository.save(record);
            }
            studentsAdded++;
        }

        return ResponseEntity.ok(Map.of("message", "Successfully seeded " + studentsAdded + " students and their academic records for risk analysis.", "added", studentsAdded));
    }
}

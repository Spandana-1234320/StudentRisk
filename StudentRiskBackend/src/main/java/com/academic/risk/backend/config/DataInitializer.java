package com.academic.risk.backend.config;

import com.academic.risk.backend.models.AcademicRecord;
import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.models.User;
import com.academic.risk.backend.repositories.AcademicRecordRepository;
import com.academic.risk.backend.repositories.AlertRepository;
import com.academic.risk.backend.repositories.RiskAssessmentRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import com.academic.risk.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;
    
    @Autowired
    private AcademicRecordRepository academicRecordRepository;
    
    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;
    
    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // Create Admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            System.out.println("✅ Admin user created: admin / admin123");
        }

        // Create Faculty user if not exists
        if (userRepository.findByUsername("faculty1").isEmpty()) {
            User faculty = new User();
            faculty.setUsername("faculty1");
            faculty.setPassword(passwordEncoder.encode("faculty123"));
            faculty.setRole(User.Role.FACULTY);
            userRepository.save(faculty);
            System.out.println("✅ Faculty user created: faculty1 / faculty123");
        }

        // Remove the two specific students if they exist (Roll numbers: CS2021001, 23EG105G43)
        List<StudentProfile> allProfiles = studentProfileRepository.findAll();
        for (StudentProfile p : allProfiles) {
            if ("CS2021001".equals(p.getRollNumber()) || "23EG105G43".equals(p.getRollNumber())) {
                academicRecordRepository.findByStudentProfile(p).forEach(ar -> academicRecordRepository.delete(ar));
                riskAssessmentRepository.findByStudentProfileOrderByAssessmentDateDesc(p).forEach(ra -> riskAssessmentRepository.delete(ra));
                alertRepository.findByStudentProfileOrderByCreatedAtDesc(p).forEach(al -> alertRepository.delete(al));
                User u = p.getUser();
                studentProfileRepository.delete(p);
                userRepository.delete(u);
                System.out.println("✅ Deleted old student: " + p.getRollNumber());
            }
        }

        // Add Top Performing Students
        addTopStudent("Ananya Sharma", "CS2021099", "ananya", "Computer Science");
        addTopStudent("Rohan Desai", "CS2021100", "rohan", "Computer Science");
        addTopStudent("Meera Reddy", "23EG105G44", "meera", "Computer Science and Engineering");
        addTopStudent("Siddharth Patel", "CS2021101", "siddharth", "Computer Science");
        addTopStudent("Priya Singh", "23EG105G45", "priya", "Information Technology");
        addTopStudent("Kabir Khan", "23EG105G46", "kabir", "Computer Science and Engineering");
        addTopStudent("Sneha Gupta", "CS2021102", "sneha", "Computer Science");
        addTopStudent("Aditya Verma", "23EG105G47", "aditya", "Information Technology");
    }
    
    private void addTopStudent(String name, String roll, String username, String dept) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User student = new User();
            student.setUsername(username);
            student.setPassword(passwordEncoder.encode("student123"));
            student.setRole(User.Role.STUDENT);
            userRepository.save(student);

            StudentProfile profile = new StudentProfile();
            profile.setUser(student);
            profile.setRollNumber(roll);
            profile.setDepartment(dept);
            profile.setCurrentSemester(4);
            studentProfileRepository.save(profile);
            
            // Add top academic record to ensure LOW risk
            AcademicRecord record = new AcademicRecord();
            record.setStudentProfile(profile);
            record.setSemester(4);
            record.setSubject("Advanced Algorithms");
            record.setAttendancePercentage(98.0);
            record.setInternalMarks(95.0);
            record.setPreviousSemesterMarks(92.0);
            record.setAssignmentSubmissionPercentage(100.0);
            record.setLateSubmissions(0);
            record.setAbsenteePatterns(0);
            academicRecordRepository.save(record);
            
            System.out.println("✅ Added top student: " + name + " (" + roll + ")");
        }
    }
}

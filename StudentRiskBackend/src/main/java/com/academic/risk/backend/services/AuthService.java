package com.academic.risk.backend.services;

import com.academic.risk.backend.dto.AuthResponse;
import com.academic.risk.backend.dto.LoginRequest;
import com.academic.risk.backend.dto.RegisterRequest;
import com.academic.risk.backend.models.FacultyProfile;
import com.academic.risk.backend.models.StudentProfile;
import com.academic.risk.backend.models.User;
import com.academic.risk.backend.repositories.FacultyProfileRepository;
import com.academic.risk.backend.repositories.StudentProfileRepository;
import com.academic.risk.backend.repositories.UserRepository;
import com.academic.risk.backend.security.JwtUtil;
import com.academic.risk.backend.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private FacultyProfileRepository facultyProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        request.setUsername(username);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        User user = userRepository.findByUsername(username).orElseThrow();
        ensureRoleProfileExists(user);

        if (request.getRole() != null && !user.getRole().name().equalsIgnoreCase(request.getRole())) {
            throw new RuntimeException("Selected role does not match user account role.");
        }
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username is required.");
        }

        request.setUsername(username);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
            userRepository.save(user);

            if (user.getRole() == User.Role.STUDENT) {
                StudentProfile profile = new StudentProfile();
                profile.setUser(user);
                profile.setRollNumber(request.getRollNumber() != null ? request.getRollNumber() : "N/A");
                profile.setDepartment(request.getDepartment() != null ? request.getDepartment() : "General");
                profile.setCurrentSemester(request.getCurrentSemester() > 0 ? request.getCurrentSemester() : 1);
                studentProfileRepository.save(profile);
            } else if (user.getRole() == User.Role.FACULTY) {
                FacultyProfile fProfile = new FacultyProfile();
                fProfile.setUser(user);
                fProfile.setSubject(null);
                fProfile.setDepartment(request.getDepartment() != null ? request.getDepartment() : "General Department");
                fProfile.setDesignation(request.getDesignation() != null ? request.getDesignation() : "Professor");
                facultyProfileRepository.save(fProfile);
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtUtil.generateToken(userDetails, user.getRole().name());
            return new AuthResponse(token, user.getUsername(), user.getRole().name());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid role selected.");
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Registration failed because the provided data conflicts with an existing record.");
        }
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
}

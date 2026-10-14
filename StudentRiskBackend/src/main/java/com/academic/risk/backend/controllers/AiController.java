package com.academic.risk.backend.controllers;

import com.academic.risk.backend.dto.ChatRequest;
import com.academic.risk.backend.dto.ChatResponse;
import com.academic.risk.backend.services.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class AiController {

    @Autowired
    private AiService aiService;

    // Allow all authenticated users (ADMIN, FACULTY, STUDENT)
    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, Authentication authentication) {
        String answer = aiService.generateResponse(request.getMessage(), authentication.getName(), request.getMessages());
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}

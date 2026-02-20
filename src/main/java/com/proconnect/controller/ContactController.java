package com.proconnect.controller;

import com.proconnect.dto.ContactMessageDTO;
import com.proconnect.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {
    
    private final ContactService contactService;
    
    @PostMapping("/professionals/{professionalId}")
    public ResponseEntity<String> sendContactMessage(
            @PathVariable Long professionalId,
            @Valid @RequestBody ContactMessageDTO dto) {
        
        contactService.sendContactMessage(professionalId, dto);
        return ResponseEntity.ok("Message sent successfully");
    }
}

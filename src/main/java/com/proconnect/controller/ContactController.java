package com.proconnect.controller;

import com.proconnect.dto.ContactMessageDTO;
import com.proconnect.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/professionals/{professionalId}")
    public ResponseEntity<String> sendContactMessage(
            @PathVariable Long professionalId,
            @Valid @RequestBody ContactMessageDTO dto) {

        log.info("POST /api/contact/professionals/{} — from={} <{}>", professionalId, dto.getName(), dto.getEmail());
        contactService.sendContactMessage(professionalId, dto);
        return ResponseEntity.ok("Message sent successfully");
    }
}

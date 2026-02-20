package com.proconnect.service;

import com.proconnect.dto.ContactMessageDTO;
import com.proconnect.entity.ContactMessage;
import com.proconnect.entity.Professional;
import com.proconnect.repository.ContactMessageRepository;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ContactService {
    
    private final ContactMessageRepository contactMessageRepository;
    private final ProfessionalRepository professionalRepository;
    
    @Transactional
    public void sendContactMessage(Long professionalId, ContactMessageDTO dto) {
        Professional professional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> new RuntimeException("Professional not found with id: " + professionalId));
        
        ContactMessage message = new ContactMessage();
        message.setProfessional(professional);
        message.setSenderName(dto.getName());
        message.setSenderEmail(dto.getEmail());
        message.setSubject(dto.getSubject());
        message.setMessage(dto.getMessage());
        message.setServiceId(dto.getServiceId());
        message.setStatus("NEW");
        
        contactMessageRepository.save(message);
        
        // TODO: Send email notification to professional
    }
}

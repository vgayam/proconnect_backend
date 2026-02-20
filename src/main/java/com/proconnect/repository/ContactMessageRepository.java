package com.proconnect.repository;

import com.proconnect.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    
    List<ContactMessage> findByProfessionalId(Long professionalId);
    
    List<ContactMessage> findByStatus(String status);
}

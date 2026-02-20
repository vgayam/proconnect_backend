package com.proconnect.repository;

import com.proconnect.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceOffering, Long> {
    
    List<ServiceOffering> findByProfessionalId(Long professionalId);
}

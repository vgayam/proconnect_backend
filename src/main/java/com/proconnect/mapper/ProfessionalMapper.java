package com.proconnect.mapper;

import com.proconnect.dto.*;
import com.proconnect.entity.Professional;
import com.proconnect.entity.ServiceOffering;
import com.proconnect.entity.Skill;
import com.proconnect.entity.SocialLink;
import com.proconnect.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProfessionalMapper {
    
    private final SkillRepository skillRepository;
    
    public ProfessionalDTO toDTO(Professional entity) {
        if (entity == null) {
            return null;
        }
        
        ProfessionalDTO dto = new ProfessionalDTO();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setDisplayName(entity.getDisplayName());
        dto.setHeadline(entity.getHeadline());
        dto.setBio(entity.getBio());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setIsVerified(entity.getIsVerified());
        dto.setIsAvailable(entity.getIsAvailable());
        dto.setRating(entity.getRating());
        dto.setReviewCount(entity.getReviewCount());
        dto.setHourlyRateMin(entity.getHourlyRateMin());
        dto.setHourlyRateMax(entity.getHourlyRateMax());
        dto.setCurrency(entity.getCurrency());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setWhatsapp(entity.getWhatsapp());
        dto.setCategory(entity.getCategory());
        
        dto.setLocation(toLocationDTO(entity));
        dto.setSkills(entity.getSkills().stream()
            .map(this::toSkillDTO)
            .collect(Collectors.toList()));
        dto.setServices(entity.getServices().stream()
            .map(this::toServiceDTO)
            .collect(Collectors.toList()));
        dto.setSocialLinks(entity.getSocialLinks().stream()
            .map(this::toSocialLinkDTO)
            .collect(Collectors.toList()));
        
        return dto;
    }
    
    public Professional toEntity(ProfessionalDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Professional entity = new Professional();
        updateEntityFromDTO(entity, dto);
        return entity;
    }
    
    public void updateEntityFromDTO(Professional entity, ProfessionalDTO dto) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setHeadline(dto.getHeadline());
        entity.setBio(dto.getBio());
        entity.setAvatarUrl(dto.getAvatarUrl());
        entity.setCoverImageUrl(dto.getCoverImageUrl());
        entity.setIsVerified(dto.getIsVerified());
        entity.setIsAvailable(dto.getIsAvailable());
        entity.setHourlyRateMin(dto.getHourlyRateMin());
        entity.setHourlyRateMax(dto.getHourlyRateMax());
        entity.setCurrency(dto.getCurrency());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setWhatsapp(dto.getWhatsapp());
        entity.setCategory(dto.getCategory());
        
        updateLocationFromDTO(entity, dto.getLocation());
        updateSkillsFromDTO(entity, dto.getSkills());
        updateServicesFromDTO(entity, dto.getServices());
        updateSocialLinksFromDTO(entity, dto.getSocialLinks());
    }
    
    private ProfessionalDTO.LocationDTO toLocationDTO(Professional entity) {
        ProfessionalDTO.LocationDTO location = new ProfessionalDTO.LocationDTO();
        location.setCity(entity.getCity());
        location.setState(entity.getState());
        location.setCountry(entity.getCountry());
        location.setRemote(entity.getRemote());
        return location;
    }
    
    private SkillDTO toSkillDTO(Skill skill) {
        return new SkillDTO(skill.getId(), skill.getName(), skill.getCategory());
    }
    
    private ServiceDTO toServiceDTO(ServiceOffering service) {
        ServiceDTO dto = new ServiceDTO();
        dto.setId(service.getId());
        dto.setTitle(service.getTitle());
        dto.setDescription(service.getDescription());
        dto.setPriceMin(service.getPriceMin());
        dto.setPriceMax(service.getPriceMax());
        dto.setCurrency(service.getCurrency());
        dto.setPriceUnit(service.getPriceUnit());
        dto.setDuration(service.getDuration());
        return dto;
    }
    
    private SocialLinkDTO toSocialLinkDTO(SocialLink link) {
        return new SocialLinkDTO(link.getId(), link.getPlatform(), link.getUrl(), link.getLabel());
    }
    
    private void updateLocationFromDTO(Professional entity, ProfessionalDTO.LocationDTO location) {
        if (location != null) {
            entity.setCity(location.getCity());
            entity.setState(location.getState());
            entity.setCountry(location.getCountry());
            entity.setRemote(location.getRemote());
        }
    }
    
    private void updateSkillsFromDTO(Professional entity, java.util.List<SkillDTO> skillDTOs) {
        if (skillDTOs != null) {
            entity.getSkills().clear();
            if (!skillDTOs.isEmpty()) {
                for (SkillDTO skillDTO : skillDTOs) {
                    Skill skill = skillRepository.findByName(skillDTO.getName())
                        .orElseGet(() -> {
                            Skill newSkill = new Skill();
                            newSkill.setName(skillDTO.getName());
                            newSkill.setCategory(skillDTO.getCategory());
                            return skillRepository.save(newSkill);
                        });
                    entity.getSkills().add(skill);
                }
            }
        }
    }
    
    private void updateServicesFromDTO(Professional entity, java.util.List<ServiceDTO> serviceDTOs) {
        if (serviceDTOs != null && !serviceDTOs.isEmpty()) {
            entity.getServices().clear();
            for (ServiceDTO serviceDTO : serviceDTOs) {
                ServiceOffering service = new ServiceOffering();
                service.setProfessional(entity);
                service.setTitle(serviceDTO.getTitle());
                service.setDescription(serviceDTO.getDescription());
                service.setPriceMin(serviceDTO.getPriceMin());
                service.setPriceMax(serviceDTO.getPriceMax());
                service.setCurrency(serviceDTO.getCurrency());
                service.setPriceUnit(serviceDTO.getPriceUnit());
                service.setDuration(serviceDTO.getDuration());
                entity.getServices().add(service);
            }
        }
    }
    
    private void updateSocialLinksFromDTO(Professional entity, java.util.List<SocialLinkDTO> linkDTOs) {
        if (linkDTOs != null && !linkDTOs.isEmpty()) {
            entity.getSocialLinks().clear();
            for (SocialLinkDTO linkDTO : linkDTOs) {
                SocialLink link = new SocialLink();
                link.setProfessional(entity);
                link.setPlatform(linkDTO.getPlatform());
                link.setUrl(linkDTO.getUrl());
                link.setLabel(linkDTO.getLabel());
                entity.getSocialLinks().add(link);
            }
        }
    }
}

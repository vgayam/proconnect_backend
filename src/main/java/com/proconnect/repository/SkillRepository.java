package com.proconnect.repository;

import com.proconnect.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    
    Optional<Skill> findByName(String name);
    
    List<Skill> findByCategory(String category);
    
    @Query("SELECT DISTINCT s.category FROM Skill s ORDER BY s.category")
    List<String> findAllCategories();
    
    @Query("SELECT s FROM Skill s ORDER BY s.name")
    List<Skill> findAllOrderByName();
}

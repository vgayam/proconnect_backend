package com.proconnect.repository;

import com.proconnect.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {
    
    List<Professional> findByCity(String city);
    
    List<Professional> findByIsAvailable(Boolean isAvailable);
    
    @Query("SELECT DISTINCT p FROM Professional p " +
           "LEFT JOIN p.skills sk " +
           "LEFT JOIN p.services sv " +
           "WHERE (:query IS NULL OR :query = '' OR " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.headline) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.bio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(sk.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(sk.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(sv.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(sv.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:city IS NULL OR :city = '' OR LOWER(p.city) = LOWER(CAST(:city AS string))) " +
           "AND (:state IS NULL OR :state = '' OR LOWER(p.state) = LOWER(CAST(:state AS string))) " +
           "AND (:country IS NULL OR :country = '' OR LOWER(p.country) = LOWER(CAST(:country AS string))) " +
           "AND (:remote IS NULL OR p.remote = :remote) " +
           "AND (:available IS NULL OR p.isAvailable = :available)")
    List<Professional> searchProfessionals(
        @Param("query") String query,
        @Param("city") String city,
        @Param("state") String state,
        @Param("country") String country,
        @Param("remote") Boolean remote,
        @Param("available") Boolean available
    );
    
    @Query("SELECT DISTINCT p FROM Professional p " +
           "JOIN p.skills s " +
           "WHERE s.name IN :skills")
    List<Professional> findBySkillsNameIn(@Param("skills") List<String> skills);
    
    @Query("SELECT DISTINCT p FROM Professional p " +
           "JOIN p.skills s " +
           "WHERE s.category IN :categories")
    List<Professional> findBySkillsCategoryIn(@Param("categories") List<String> categories);

    // Matches on professional.category (primary) OR skill.category (fallback)
    @Query("SELECT DISTINCT p FROM Professional p " +
           "LEFT JOIN p.skills sk " +
           "WHERE p.category IN :categories OR sk.category IN :categories")
    List<Professional> findByCategoryOrSkillsCategory(@Param("categories") List<String> categories);
    
    @Query("SELECT DISTINCT p FROM Professional p " +
           "LEFT JOIN p.skills sk " +
           "WHERE (:skillQuery IS NULL OR :skillQuery = '' OR " +
           "LOWER(sk.name) LIKE LOWER(CONCAT('%', :skillQuery, '%')) OR " +
           "LOWER(sk.category) LIKE LOWER(CONCAT('%', :skillQuery, '%')))")
    List<Professional> searchBySkillsOrCategory(@Param("skillQuery") String skillQuery);

    // Returns all distinct non-null cities, sorted alphabetically
    @Query("SELECT DISTINCT p.city FROM Professional p WHERE p.city IS NOT NULL AND p.city <> '' ORDER BY p.city")
    List<String> findDistinctCities();
}

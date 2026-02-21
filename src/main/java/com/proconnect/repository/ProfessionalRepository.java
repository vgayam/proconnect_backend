package com.proconnect.repository;

import com.proconnect.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    Optional<Professional> findBySlug(String slug);

    List<Professional> findByCity(String city);

    List<Professional> findByIsAvailable(Boolean isAvailable);

    // ─────────────────────────────────────────────────────────────────────────
    // Full-text + trigram search  (main search path — native PostgreSQL)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Primary FTS search using search_vector with ts_rank scoring.
     * Falls back to trigram similarity on name/city when query is blank.
     */
    @Query(nativeQuery = true, value = """
        SELECT DISTINCT p.*,
               CASE WHEN :query IS NOT NULL AND :query <> ''
                    THEN ts_rank(p.search_vector, plainto_tsquery('english', :query))
                    ELSE 0.0
               END AS rank
        FROM professionals p
        LEFT JOIN professional_subcategories ps ON ps.professional_id = p.id
        LEFT JOIN subcategories sc               ON sc.id = ps.subcategory_id
        WHERE 1=1
          AND (:query     IS NULL OR :query     = '' OR p.search_vector @@ plainto_tsquery('english', :query))
          AND (:city      IS NULL OR :city      = '' OR similarity(p.city,    :city)    > 0.25)
          AND (:state     IS NULL OR :state     = '' OR LOWER(p.state)    = LOWER(:state))
          AND (:country   IS NULL OR :country   = '' OR LOWER(p.country)  = LOWER(:country))
          AND (:remote    IS NULL OR p.remote              = :remote)
          AND (:available IS NULL OR p.is_available        = :available)
          AND (:category  IS NULL OR :category  = '' OR LOWER(p.category) = LOWER(:category))
        ORDER BY rank DESC, p.rating DESC NULLS LAST
        LIMIT :pageSize OFFSET :offset
        """)
    List<Professional> searchProfessionals(
        @Param("query")     String  query,
        @Param("city")      String  city,
        @Param("state")     String  state,
        @Param("country")   String  country,
        @Param("remote")    Boolean remote,
        @Param("available") Boolean available,
        @Param("category")  String  category,
        @Param("pageSize")  int     pageSize,
        @Param("offset")    int     offset
    );

    /** Count query for pagination total */
    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT p.id)
        FROM professionals p
        LEFT JOIN professional_subcategories ps ON ps.professional_id = p.id
        LEFT JOIN subcategories sc               ON sc.id = ps.subcategory_id
        WHERE 1=1
          AND (:query     IS NULL OR :query     = '' OR p.search_vector @@ plainto_tsquery('english', :query))
          AND (:city      IS NULL OR :city      = '' OR similarity(p.city,    :city)    > 0.25)
          AND (:state     IS NULL OR :state     = '' OR LOWER(p.state)    = LOWER(:state))
          AND (:country   IS NULL OR :country   = '' OR LOWER(p.country)  = LOWER(:country))
          AND (:remote    IS NULL OR p.remote              = :remote)
          AND (:available IS NULL OR p.is_available        = :available)
          AND (:category  IS NULL OR :category  = '' OR LOWER(p.category) = LOWER(:category))
        """)
    long countSearchProfessionals(
        @Param("query")     String  query,
        @Param("city")      String  city,
        @Param("state")     String  state,
        @Param("country")   String  country,
        @Param("remote")    Boolean remote,
        @Param("available") Boolean available,
        @Param("category")  String  category
    );

    /** FTS + subcategory name filter */
    @Query(nativeQuery = true, value = """
        SELECT DISTINCT p.*,
               CASE WHEN :query IS NOT NULL AND :query <> ''
                    THEN ts_rank(p.search_vector, plainto_tsquery('english', :query))
                    ELSE 0.0
               END AS rank
        FROM professionals p
        JOIN professional_subcategories ps ON ps.professional_id = p.id
        JOIN subcategories sc               ON sc.id = ps.subcategory_id
        WHERE LOWER(sc.name) = ANY(LOWER(CAST(:subcategoryNames AS TEXT))\\:\\:TEXT[])
          AND (:query     IS NULL OR :query     = '' OR p.search_vector @@ plainto_tsquery('english', :query))
          AND (:city      IS NULL OR :city      = '' OR similarity(p.city, :city) > 0.25)
          AND (:available IS NULL OR p.is_available = :available)
        ORDER BY rank DESC, p.rating DESC NULLS LAST
        LIMIT :pageSize OFFSET :offset
        """)
    List<Professional> searchBySubcategoryNames(
        @Param("subcategoryNames") String  subcategoryNamesArray,
        @Param("query")            String  query,
        @Param("city")             String  city,
        @Param("available")        Boolean available,
        @Param("pageSize")         int     pageSize,
        @Param("offset")           int     offset
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Facet queries
    // ─────────────────────────────────────────────────────────────────────────

    @Query(nativeQuery = true, value = """
        SELECT p.category AS name, COUNT(DISTINCT p.id) AS cnt
        FROM professionals p
        WHERE p.category IS NOT NULL AND p.category <> ''
        GROUP BY p.category
        ORDER BY cnt DESC
        LIMIT 20
        """)
    List<Object[]> facetsByCategory();

    @Query(nativeQuery = true, value = """
        SELECT p.city AS name, COUNT(DISTINCT p.id) AS cnt
        FROM professionals p
        WHERE p.city IS NOT NULL AND p.city <> ''
        GROUP BY p.city
        ORDER BY cnt DESC
        LIMIT 20
        """)
    List<Object[]> facetsByCity();

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy / convenience
    // ─────────────────────────────────────────────────────────────────────────

    @Query("SELECT DISTINCT p FROM Professional p JOIN p.subcategories s WHERE s.name IN :names")
    List<Professional> findBySubcategoriesNameIn(@Param("names") List<String> names);

    @Query("SELECT DISTINCT p FROM Professional p JOIN p.subcategories s WHERE s.category IN :categories")
    List<Professional> findBySubcategoriesCategoryIn(@Param("categories") List<String> categories);

    @Query("SELECT DISTINCT p FROM Professional p LEFT JOIN p.subcategories sc " +
           "WHERE p.category IN :categories OR sc.category IN :categories")
    List<Professional> findByCategoryOrSubcategoriesCategory(@Param("categories") List<String> categories);

    @Query("SELECT DISTINCT p.city FROM Professional p WHERE p.city IS NOT NULL AND p.city <> '' ORDER BY p.city")
    List<String> findDistinctCities();
}

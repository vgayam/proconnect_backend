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

    Optional<Professional> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Professional> findByCity(String city);

    List<Professional> findByIsAvailable(Boolean isAvailable);

    @Query("SELECT DISTINCT p.city FROM Professional p WHERE p.city IS NOT NULL AND p.city <> '' ORDER BY p.city")
    List<String> findDistinctCities();

    // ─────────────────────────────────────────────────────────────────────────
    // Unified search  (single query handles all filter combinations)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * One query to rule them all.
     *
     * Filters (all optional / nullable):
     *   :query    — FTS + pg_trgm fuzzy keyword search
     *   :city     — fuzzy city match
     *   :state    — exact state match
     *   :country  — exact country match
     *   :remote   — boolean flag
     *   :available — boolean flag
     *   :category — exact category match
     *   :area     — neighbourhood / locality (service area) — NULL = no area filter
     *   :subcategoryNames — pg text-array literal e.g. '{plumbing,tiling}' — NULL = no filter
     */
    @Query(nativeQuery = true, value = """
        SELECT p.*
        FROM professionals p
        LEFT JOIN professional_subcategories ps ON ps.professional_id = p.id
        LEFT JOIN subcategories sc               ON sc.id = ps.subcategory_id
        LEFT JOIN professional_service_areas psa ON psa.professional_id = p.id
        WHERE 1=1
          AND (
              :query IS NULL OR :query = ''
              OR p.search_vector @@ plainto_tsquery('english', :query)
              OR word_similarity(:query, p.headline)           > 0.3
              OR word_similarity(:query, p.category)           > 0.3
              OR word_similarity(:query, coalesce(p.bio,''))   > 0.25
              OR word_similarity(:query, coalesce(sc.name,'')) > 0.3
          )
          AND (:city             IS NULL OR :city    = '' OR word_similarity(:city,   p.city)   > 0.4)
          AND (:state            IS NULL OR :state   = '' OR LOWER(p.state)   = LOWER(:state))
          AND (:country          IS NULL OR :country = '' OR LOWER(p.country) = LOWER(:country))
          AND (:remote           IS NULL OR p.remote       = :remote)
          AND (:available        IS NULL OR p.is_available = :available)
          AND (:category         IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category))
          AND (:area             IS NULL OR :area    = '' OR word_similarity(:area, LOWER(psa.area_name)) > 0.3)
          AND (:subcategoryNames IS NULL OR LOWER(sc.name) = ANY(LOWER(CAST(:subcategoryNames AS TEXT))\\:\\:TEXT[]))
        GROUP BY p.id
        ORDER BY
          MAX(COALESCE(ts_rank(p.search_vector, plainto_tsquery('english', COALESCE(:query,''))), 0.0) +
              word_similarity(COALESCE(:query,''), p.headline) * 0.5) DESC,
          p.rating DESC NULLS LAST
        LIMIT :pageSize OFFSET :offset
        """)
    List<Professional> searchProfessionals(
        @Param("query")             String  query,
        @Param("city")              String  city,
        @Param("state")             String  state,
        @Param("country")           String  country,
        @Param("remote")            Boolean remote,
        @Param("available")         Boolean available,
        @Param("category")          String  category,
        @Param("area")              String  area,
        @Param("subcategoryNames")  String  subcategoryNames,
        @Param("pageSize")          int     pageSize,
        @Param("offset")            int     offset
    );

    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT p.id)
        FROM professionals p
        LEFT JOIN professional_subcategories ps ON ps.professional_id = p.id
        LEFT JOIN subcategories sc               ON sc.id = ps.subcategory_id
        LEFT JOIN professional_service_areas psa ON psa.professional_id = p.id
        WHERE 1=1
          AND (
              :query IS NULL OR :query = ''
              OR p.search_vector @@ plainto_tsquery('english', :query)
              OR word_similarity(:query, p.headline)           > 0.3
              OR word_similarity(:query, p.category)           > 0.3
              OR word_similarity(:query, coalesce(p.bio,''))   > 0.25
              OR word_similarity(:query, coalesce(sc.name,'')) > 0.3
          )
          AND (:city              IS NULL OR :city     = '' OR word_similarity(:city,  p.city)   > 0.4)
          AND (:state             IS NULL OR :state    = '' OR LOWER(p.state)    = LOWER(:state))
          AND (:country           IS NULL OR :country  = '' OR LOWER(p.country)  = LOWER(:country))
          AND (:remote            IS NULL OR p.remote            = :remote)
          AND (:available         IS NULL OR p.is_available      = :available)
          AND (:category          IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category))
          AND (:area              IS NULL OR :area     = '' OR word_similarity(:area, LOWER(psa.area_name)) > 0.3)
          AND (:subcategoryNames  IS NULL OR LOWER(sc.name) = ANY(LOWER(CAST(:subcategoryNames AS TEXT))\\:\\:TEXT[]))
        """)
    long countSearchProfessionals(
        @Param("query")             String  query,
        @Param("city")              String  city,
        @Param("state")             String  state,
        @Param("country")           String  country,
        @Param("remote")            Boolean remote,
        @Param("available")         Boolean available,
        @Param("category")          String  category,
        @Param("area")              String  area,
        @Param("subcategoryNames")  String  subcategoryNames
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Facet queries
    // ─────────────────────────────────────────────────────────────────────────

    @Query(nativeQuery = true, value = """
        SELECT p.category AS name, COUNT(DISTINCT p.id) AS cnt
        FROM professionals p
        WHERE p.category IS NOT NULL AND p.category <> ''
        GROUP BY p.category ORDER BY cnt DESC LIMIT 20
        """)
    List<Object[]> facetsByCategory();

    @Query(nativeQuery = true, value = """
        SELECT p.city AS name, COUNT(DISTINCT p.id) AS cnt
        FROM professionals p
        WHERE p.city IS NOT NULL AND p.city <> ''
        GROUP BY p.city ORDER BY cnt DESC LIMIT 20
        """)
    List<Object[]> facetsByCity();

    @Query(nativeQuery = true, value = """
        SELECT psa.area_name AS name, COUNT(DISTINCT psa.professional_id) AS cnt
        FROM professional_service_areas psa
        WHERE psa.area_name IS NOT NULL AND psa.area_name <> ''
        GROUP BY psa.area_name ORDER BY cnt DESC LIMIT 30
        """)
    List<Object[]> facetsByServiceArea();

    // ─────────────────────────────────────────────────────────────────────────
    // Area name lookup (for natural-language query parsing)
    // ─────────────────────────────────────────────────────────────────────────

    @Query(nativeQuery = true, value = """
        SELECT psa.area_name
        FROM professional_service_areas psa
        WHERE similarity(LOWER(psa.area_name), LOWER(:hint)) > 0.25
        GROUP BY psa.area_name
        ORDER BY MAX(similarity(LOWER(psa.area_name), LOWER(:hint))) DESC
        LIMIT 1
        """)
    List<String> findMatchingAreaName(@Param("hint") String hint);
}

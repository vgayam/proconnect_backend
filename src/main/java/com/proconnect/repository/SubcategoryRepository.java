package com.proconnect.repository;

import com.proconnect.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    Optional<Subcategory> findByName(String name);

    @Query("SELECT s FROM Subcategory s WHERE s.category.name = :categoryName ORDER BY s.name")
    List<Subcategory> findByCategoryName(@Param("categoryName") String categoryName);

    @Query("SELECT DISTINCT s.category.name FROM Subcategory s ORDER BY s.category.name")
    List<String> findAllCategories();

    @Query("SELECT s FROM Subcategory s ORDER BY s.name")
    List<Subcategory> findAllOrderByName();
}

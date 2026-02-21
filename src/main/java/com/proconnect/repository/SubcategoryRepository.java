package com.proconnect.repository;

import com.proconnect.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    Optional<Subcategory> findByName(String name);

    List<Subcategory> findByCategory(String category);

    @Query("SELECT DISTINCT s.category FROM Subcategory s ORDER BY s.category")
    List<String> findAllCategories();

    @Query("SELECT s FROM Subcategory s ORDER BY s.name")
    List<Subcategory> findAllOrderByName();
}

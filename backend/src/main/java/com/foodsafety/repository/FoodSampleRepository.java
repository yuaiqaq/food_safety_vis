package com.foodsafety.repository;

import com.foodsafety.entity.FoodSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodSampleRepository extends JpaRepository<FoodSample, Long> {

    List<FoodSample> findByProductionLocation(String productionLocation);

    List<FoodSample> findByFoodCategory(String foodCategory);

    List<FoodSample> findByAdulterantCategory(String adulterantCategory);

    List<FoodSample> findByGrade(Integer grade);

    @Query("SELECT f FROM FoodSample f WHERE " +
           "(:region IS NULL OR f.productionLocation = :region) AND " +
           "(:category IS NULL OR f.foodCategory = :category) AND " +
           "(:adulterantCategory IS NULL OR f.adulterantCategory = :adulterantCategory) AND " +
           "(:grade IS NULL OR f.grade = :grade)")
    List<FoodSample> findByFilters(
            @Param("region") String region,
            @Param("category") String category,
            @Param("adulterantCategory") String adulterantCategory,
            @Param("grade") Integer grade
    );

    @Query("SELECT DISTINCT f.productionLocation FROM FoodSample f ORDER BY f.productionLocation")
    List<String> findDistinctRegions();

    @Query("SELECT DISTINCT f.foodCategory FROM FoodSample f ORDER BY f.foodCategory")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT f.adulterantCategory FROM FoodSample f ORDER BY f.adulterantCategory")
    List<String> findDistinctAdulterantCategories();

    @Query("SELECT f.productionLocation, COUNT(f) FROM FoodSample f GROUP BY f.productionLocation ORDER BY COUNT(f) DESC")
    List<Object[]> countByRegion();

    @Query("SELECT f.foodCategory, COUNT(f) FROM FoodSample f GROUP BY f.foodCategory ORDER BY COUNT(f) DESC")
    List<Object[]> countByCategory();

    @Query("SELECT f.adulterantCategory, COUNT(f) FROM FoodSample f GROUP BY f.adulterantCategory ORDER BY COUNT(f) DESC")
    List<Object[]> countByAdulterantCategory();

    @Query("SELECT f.adulterant, COUNT(f) FROM FoodSample f GROUP BY f.adulterant ORDER BY COUNT(f) DESC")
    List<Object[]> countByAdulterant();

    @Query("SELECT f.grade, COUNT(f) FROM FoodSample f GROUP BY f.grade ORDER BY f.grade")
    List<Object[]> countByGrade();
}

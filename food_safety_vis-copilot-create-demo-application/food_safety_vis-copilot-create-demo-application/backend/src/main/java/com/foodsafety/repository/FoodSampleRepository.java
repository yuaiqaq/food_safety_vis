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
            // AND 模式：所有条件 必须同时满足
            "(:mode = 'AND' " +
            " AND (:region IS NULL OR f.productionLocation = :region) " +
            " AND (:category IS NULL OR f.foodCategory = :category) " +
            " AND (:adulterantCategory IS NULL OR f.adulterantCategory = :adulterantCategory) " +
            " AND (:adulterants IS NULL OR f.adulterants = :adulterants)) " +

            " OR " +

            // OR 模式：任意一个条件满足即可
            "(:mode = 'OR' " +
            " AND (:region IS NULL OR f.productionLocation = :region) " +
            " AND (:category IS NULL OR f.foodCategory = :category) " +
            " AND (:adulterantCategory IS NULL OR f.adulterantCategory = :adulterantCategory) " +
            " AND (:adulterants IS NULL OR f.adulterants = :adulterants)) ")
    List<FoodSample> findByFilters(
            @Param("region") String region,
            @Param("category") String category,
            @Param("adulterantCategory") String adulterantCategory,
            @Param("adulterants") String adulterants,
            @Param("mode") String mode
    );

    @Query("SELECT f FROM FoodSample f WHERE " +
            "(:mode = 'AND' " +
            " AND (:regions IS NULL OR f.productionLocation IN (:regions)) " +
            " AND (:categories IS NULL OR f.foodCategory IN (:categories)) " +
            " AND (:adulterantCategories IS NULL OR f.adulterantCategory IN (:adulterantCategories)) " +
            " AND (:adulterants IS NULL OR f.adulterants IN (:adulterants)) " +
            ") " +
            "OR " +
            "(:mode = 'OR' " +
            " AND (" +
            "       (:regions IS NOT NULL AND f.productionLocation IN (:regions)) " +
            "    OR (:categories IS NOT NULL AND f.foodCategory IN (:categories)) " +
            "    OR (:adulterantCategories IS NOT NULL AND f.adulterantCategory IN (:adulterantCategories)) " +
            "    OR (:adulterants IS NOT NULL AND f.adulterants IN (:adulterants)) " +
            " ))")
    List<FoodSample> findByCategory(
            @Param("regions") List<String> regions,
            @Param("categories") List<String> categories,
            @Param("adulterantCategories") List<String> adulterantCategories,
            @Param("adulterants") List<String> adulterants,
            @Param("mode") String mode
    );

    @Query("SELECT DISTINCT f.productionLocation FROM FoodSample f ORDER BY f.productionLocation")
    List<String> findDistinctRegions();

    @Query("SELECT DISTINCT f.foodCategory FROM FoodSample f ORDER BY f.foodCategory")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT f.adulterantCategory FROM FoodSample f ORDER BY f.adulterantCategory")
    List<String> findDistinctAdulterantCategories();

    @Query("SELECT DISTINCT f.adulterants FROM FoodSample f ORDER BY f.adulterants")
    List<String> findDistinctAdulterants();

    @Query("SELECT f.productionLocation, COUNT(f) FROM FoodSample f GROUP BY f.productionLocation ORDER BY COUNT(f) DESC")
    List<Object[]> countByRegion();

    @Query("SELECT f.foodCategory, COUNT(f) FROM FoodSample f GROUP BY f.foodCategory ORDER BY COUNT(f) DESC")
    List<Object[]> countByCategory();

    @Query("SELECT f.adulterantCategory, COUNT(f) FROM FoodSample f GROUP BY f.adulterantCategory ORDER BY COUNT(f) DESC")
    List<Object[]> countByAdulterantCategory();

    @Query("SELECT f.adulterants, COUNT(f) FROM FoodSample f GROUP BY f.adulterants ORDER BY COUNT(f) DESC")
    List<Object[]> countByAdulterant();

    @Query("SELECT f.grade, COUNT(f) FROM FoodSample f WHERE " +
            "(:region IS NULL OR f.productionLocation = :region) AND " +
            "(:category IS NULL OR f.foodCategory = :category) AND " +
            "(:adulterantCategory IS NULL OR f.adulterantCategory = :adulterantCategory) AND " +
            "(:adulterants IS NULL OR f.adulterants = :adulterants) " +
            "GROUP BY f.grade ORDER BY f.grade")
    List<Object[]> countByGrade(
            @Param("region") String region,
            @Param("category") String category,
            @Param("adulterantCategory") String adulterantCategory,
            @Param("adulterants") String adulterants);
}

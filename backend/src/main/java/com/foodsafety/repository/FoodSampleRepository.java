package com.foodsafety.repository;

import com.foodsafety.entity.FoodSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodSampleRepository extends JpaRepository<FoodSample, Long> {

    List<FoodSample> findByRegion(String region);

    List<FoodSample> findByCategory(String category);

    List<FoodSample> findByQualified(Boolean qualified);

    List<FoodSample> findByRiskLevel(String riskLevel);

    List<FoodSample> findByRegionAndCategory(String region, String category);

    @Query("SELECT DISTINCT f.region FROM FoodSample f")
    List<String> findDistinctRegions();

    @Query("SELECT DISTINCT f.category FROM FoodSample f")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT f.additive FROM FoodSample f WHERE f.additive IS NOT NULL AND f.additive != ''")
    List<String> findDistinctAdditives();
}

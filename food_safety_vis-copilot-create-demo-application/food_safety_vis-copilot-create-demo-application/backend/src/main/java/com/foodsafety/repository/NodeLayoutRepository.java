package com.foodsafety.repository;

import com.foodsafety.entity.NodeLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NodeLayoutRepository extends JpaRepository<NodeLayout, Long> {

    List<NodeLayout> findByFilterKey(String filterKey);

    @Transactional   // 必须加
    @Modifying
    @Query("DELETE FROM NodeLayout n WHERE n.filterKey = :filterKey")
    void deleteByFilterKey(@Param("filterKey") String filterKey);
}
package com.foodsafety.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "food_sample")
public class FoodSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sample code, e.g. FS-2024-001 */
    private String sampleCode;

    /** Product name */
    private String foodName;

    /** Production region: 华东/华北/华南/西南/东北 */
    private String region;

    /** Food category: 乳制品/肉制品/蔬菜及制品/水果及制品/饮料/粮食及加工品 */
    private String category;

    /** Subcategory */
    private String subcategory;

    /** Additive used (may be empty for qualified samples) */
    private String additive;

    /** Production season: 春/夏/秋/冬 */
    private String season;

    /** Production batch */
    private String batch;

    /** Whether the sample passed inspection */
    private Boolean qualified;

    /** Risk level: 高/中/低 */
    private String riskLevel;

    /**
     * Comma-separated list of non-compliance items,
     * e.g. "菌落总数超标,农药残留超标"
     */
    @Column(length = 500)
    private String nonComplianceItems;

    /** Severity score 1-10 */
    private Integer severityScore;

    /** Sample date (year-month) */
    private String sampleDate;
}

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
    private Long id;

    /** Province of production, e.g. 广东省 */
    private String productionLocation;

    /** City of production, e.g. 广州市 */
    private String productionLocation2;

    /** Province of sale */
    private String saleLocation;

    /** City of sale */
    private String saleLocation2;

    /** Same Place / Off-site */
    private String typesOfSampling;

    /** Food category, e.g. pastry, alcohol, meat and meat product */
    private String foodCategory;

    /** Adulterant category: Specification / Food additive / Microbial contamination /
     *  Environmental contaminant / Pesticide and veterinary drug / Toxin */
    private String adulterantCategory;

    /** Specific adulterant name, e.g. 菌落总数, 过氧化值 */
    @Column(length = 500)
    private String adulterant;

    /** Pre-packaged / Bulk weighing */
    private String foodSpecModel;

    /** Provincially Mandated / Nationally Mandated */
    private String mandateLevel;

    /** manufacturer / eatery / restaurant / etc. */
    private String manufacturerType;

    /** Sampled location type */
    private String sampledLocationType;

    /**
     * Non-compliance severity grade:
     * 0=minor, 1=light, 2=medium, 3=severe
     */
    private Integer grade;
}

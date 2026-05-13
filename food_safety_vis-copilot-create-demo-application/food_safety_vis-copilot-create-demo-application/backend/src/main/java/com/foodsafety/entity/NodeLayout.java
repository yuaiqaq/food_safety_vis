package com.foodsafety.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "network_layout")
public class NodeLayout {  // 👈 就用这个名字

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filterKey;
    private String nodeId;
    private Double x;
    private Double y;
}
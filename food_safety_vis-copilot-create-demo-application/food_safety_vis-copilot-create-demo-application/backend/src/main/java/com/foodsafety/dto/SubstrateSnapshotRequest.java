package com.foodsafety.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubstrateSnapshotRequest {
    private List<SnapshotNodePosition> nodes;
}

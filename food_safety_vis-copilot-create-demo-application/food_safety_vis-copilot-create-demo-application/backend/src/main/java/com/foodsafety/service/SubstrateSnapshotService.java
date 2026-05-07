package com.foodsafety.service;

import com.foodsafety.dto.SnapshotNodePosition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
public class SubstrateSnapshotService {

    private static final String HEADER = "id,x,y,updatedAt";
    private final Path snapshotPath;

    public SubstrateSnapshotService(
            @Value("${snapshot.substrate.file:./data/substrate-layout-snapshot.csv}") String snapshotFilePath) {
        this.snapshotPath = Paths.get(snapshotFilePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Path parent = snapshotPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(snapshotPath)) {
                Files.write(snapshotPath, List.of(HEADER), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize substrate snapshot file: " + snapshotPath, e);
        }
    }

    public synchronized Map<String, SnapshotNodePosition> loadPositions(Collection<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> idSet = new HashSet<>(nodeIds);
        Map<String, SnapshotNodePosition> all = readAll();
        Map<String, SnapshotNodePosition> filtered = new HashMap<>();
        for (String id : idSet) {
            SnapshotNodePosition position = all.get(id);
            if (position != null) {
                filtered.put(id, position);
            }
        }
        return filtered;
    }

    public synchronized void savePositions(List<SnapshotNodePosition> positions) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        Map<String, SnapshotNodePosition> merged = readAll();
        for (SnapshotNodePosition position : positions) {
            if (position == null || position.getId() == null || position.getId().isBlank()
                    || position.getX() == null || position.getY() == null
                    || !Double.isFinite(position.getX()) || !Double.isFinite(position.getY())) {
                continue;
            }
            SnapshotNodePosition normalized = new SnapshotNodePosition();
            normalized.setId(position.getId().trim());
            normalized.setX(position.getX());
            normalized.setY(position.getY());
            merged.put(normalized.getId(), normalized);
        }
        writeAll(merged);
    }

    private Map<String, SnapshotNodePosition> readAll() {
        if (!Files.exists(snapshotPath)) {
            return new HashMap<>();
        }
        try {
            List<String> lines = Files.readAllLines(snapshotPath, StandardCharsets.UTF_8);
            Map<String, SnapshotNodePosition> map = new HashMap<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 4);
                if (parts.length < 3) continue;
                try {
                    SnapshotNodePosition position = new SnapshotNodePosition();
                    position.setId(parts[0]);
                    position.setX(Double.parseDouble(parts[1]));
                    position.setY(Double.parseDouble(parts[2]));
                    map.put(parts[0], position);
                } catch (NumberFormatException ignored) {
                }
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read substrate snapshot file: " + snapshotPath, e);
        }
    }

    private void writeAll(Map<String, SnapshotNodePosition> positions) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        String now = Instant.now().toString();
        positions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    SnapshotNodePosition p = entry.getValue();
                    lines.add(String.join(",",
                            entry.getKey(),
                            String.valueOf(p.getX()),
                            String.valueOf(p.getY()),
                            now));
                });
        try {
            Files.write(snapshotPath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write substrate snapshot file: " + snapshotPath, e);
        }
    }
}

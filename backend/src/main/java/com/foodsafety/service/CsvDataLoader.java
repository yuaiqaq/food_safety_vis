package com.foodsafety.service;

import com.foodsafety.entity.FoodSample;
import com.foodsafety.repository.FoodSampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads filtered_samples.csv into the H2 in-memory database on startup.
 * The CSV file must be placed in src/main/resources/.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvDataLoader implements CommandLineRunner {

    private final FoodSampleRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() > 0) {
            log.info("Database already populated, skipping CSV load.");
            return;
        }

        ClassPathResource resource = new ClassPathResource("filtered_samples.csv");
        if (!resource.exists()) {
            log.warn("filtered_samples.csv not found in classpath. Skipping CSV load.");
            return;
        }

        List<FoodSample> samples = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // skip header (may have BOM)
            int lineNum = 1;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    FoodSample sample = parseLine(line);
                    if (sample != null) {
                        samples.add(sample);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {} - {}", lineNum, line, e.getMessage());
                }

                // Batch save every 500 records
                if (samples.size() >= 500) {
                    repository.saveAll(samples);
                    samples.clear();
                }
            }

            if (!samples.isEmpty()) {
                repository.saveAll(samples);
            }
        }

        log.info("CSV loaded. Total records: {}", repository.count());
    }

    private FoodSample parseLine(String line) {
        // Simple CSV split — fields don't contain commas based on data inspection
        String[] cols = line.split(",", -1);
        if (cols.length < 14) return null;

        FoodSample s = new FoodSample();
        s.setId(parseLong(cols[0].trim()));
        s.setProductionLocation(cols[1].trim());
        s.setProductionLocation2(cols[2].trim());
        s.setSaleLocation(cols[3].trim());
        s.setSaleLocation2(cols[4].trim());
        s.setTypesOfSampling(cols[5].trim());
        s.setFoodCategory(cols[6].trim());
        s.setAdulterantCategory(cols[7].trim());
        s.setAdulterant(cols[8].trim());
        s.setFoodSpecModel(cols[9].trim());
        s.setMandateLevel(cols[10].trim());
        s.setManufacturerType(cols[11].trim());
        s.setSampledLocationType(cols[12].trim());
        s.setGrade(parseInt(cols[13].trim()));
        return s;
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}

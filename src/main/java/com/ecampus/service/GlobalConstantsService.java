package com.ecampus.service;

import com.ecampus.model.Terms;
import com.ecampus.repository.AcademicYearsRepository;
import com.ecampus.repository.TermsRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Service
public class GlobalConstantsService {

    private static final Path GLOBAL_CONSTANTS_FILE =
            Path.of("src", "main", "resources", "global-constants.properties");

    private final AcademicYearsRepository academicYearsRepository;
    private final TermsRepository termsRepository;
    private final Properties properties = new Properties();

    public GlobalConstantsService(AcademicYearsRepository academicYearsRepository,
                                  TermsRepository termsRepository) {
        this.academicYearsRepository = academicYearsRepository;
        this.termsRepository = termsRepository;
    }

    @PostConstruct
    public void load() {
        reloadFromFile();
    }

    public synchronized void reloadFromFile() {
        properties.clear();
        try (InputStream input = Files.newInputStream(GLOBAL_CONSTANTS_FILE)) {
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load global constants file.", ex);
        }
    }

    public synchronized void updateAcademicContext(String academicYearName, String termName) {
        String normalizedAcademicYear = normalizeRequired(academicYearName, "Academic year");
        String normalizedTermName = normalizeRequired(termName, "Term name");

        Long academicYearId = academicYearsRepository.findAcademicYearIdByName(normalizedAcademicYear);
        if (academicYearId == null) {
            throw new IllegalArgumentException("Academic year not found: " + normalizedAcademicYear);
        }

        Terms term = termsRepository.findByYearAndTerm(normalizedAcademicYear, normalizedTermName);
        if (term == null) {
            throw new IllegalArgumentException(
                    "Term not found for academic year " + normalizedAcademicYear + ": " + normalizedTermName);
        }

        properties.setProperty("ecampus.academic.current-academic-year-id", academicYearId.toString());
        properties.setProperty("ecampus.academic.current-academic-year-name", normalizedAcademicYear);
        properties.setProperty("ecampus.academic.current-term-id", term.getTrmid().toString());
        properties.setProperty("ecampus.academic.current-term-name", term.getTrmname());
        storeToFile();
    }

    public synchronized Long getCurrentAcademicYearId() {
        return getLong("ecampus.academic.current-academic-year-id");
    }

    public synchronized String getCurrentAcademicYearName() {
        return properties.getProperty("ecampus.academic.current-academic-year-name", "");
    }

    public synchronized Long getCurrentTermId() {
        return getLong("ecampus.academic.current-term-id");
    }

    public synchronized String getCurrentTermName() {
        return properties.getProperty("ecampus.academic.current-term-name", "");
    }

    public synchronized String getFacultyRole() {
        return properties.getProperty("ecampus.roles.faculty");
    }

    public synchronized String getStudentRole() {
        return properties.getProperty("ecampus.roles.student");
    }

    public synchronized String getAdminRole() {
        return properties.getProperty("ecampus.roles.admin");
    }

    public synchronized String getDeanApRole() {
        return properties.getProperty("ecampus.roles.dean-ap");
    }

    public synchronized String getRegistrarRole() {
        return properties.getProperty("ecampus.roles.registrar");
    }

    private void storeToFile() {
        try (OutputStream output = Files.newOutputStream(GLOBAL_CONSTANTS_FILE)) {
            properties.store(output, "Global application-level constants");
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to update global constants file.", ex);
        }
    }

    private Long getLong(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value.trim());
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}


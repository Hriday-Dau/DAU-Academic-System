package com.ecampus.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ecampus.model.StudentGradeDTO;
import com.ecampus.model.TermCourses;
import com.ecampus.model.Terms;
import com.ecampus.repository.TermCoursesRepository;
import com.ecampus.service.FileService;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private TermCoursesRepository termCoursesRepository;

    @Value("${file.upload.dir:uploads/courses}")
    private String uploadDir;

    @Override
    public boolean hasCsvFormat(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        return "text/csv".equals(file.getContentType()) || filename.endsWith(".csv");
    }

    @Override
    public List<StudentGradeDTO> csvToStudentGradeDTOs(InputStream inputStream) {
        List<StudentGradeDTO> grades = new ArrayList<>();

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build())) {

            for (CSVRecord record : parser) {
                String studentId = getColumnValue(record, "studentid", "student_id", "studentinstid", "stdinstid");
                String grade = getColumnValue(record, "grade", "std_grade", "gradevalue");

                if (studentId == null || grade == null || !isValidGrade(grade)) {
                    continue;
                }

                StudentGradeDTO dto = new StudentGradeDTO();
                dto.setStudentId(studentId.trim());
                dto.setStudentName(studentId.trim());
                dto.setGrade(grade.trim().toUpperCase());
                dto.setModifiedGrade(grade.trim().toUpperCase());
                dto.setSelectedForUpdate(true);
                grades.add(dto);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse CSV file: " + ex.getMessage(), ex);
        }

        return grades;
    }

    private String getColumnValue(CSVRecord record, String... names) {
        for (String name : names) {
            if (record.isMapped(name)) {
                String value = record.get(name);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private boolean isValidGrade(String grade) {
        String normalized = grade == null ? "" : grade.trim().toUpperCase();
        return List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F", "I", "NULL", "A", "B", "C", "D", "E")
                .contains(normalized);
    }

    @Override
    public boolean hasPdfFormat(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        return "application/pdf".equals(file.getContentType()) || filename.endsWith(".pdf");
    }

    @Override
    public String getUploadDirectory() {
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), uploadDir);
        }
        return path.toString();
    }

    @Override
    public Map<String, Object> uploadCoursePdf(MultipartFile file, Long tcrid, String courseCode) throws Exception {
        if (!hasPdfFormat(file)) {
            throw new IllegalArgumentException("Invalid file format. Please upload a PDF file.");
        }
        if (tcrid == null || courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Missing term-course or course code.");
        }

        TermCourses termCourse = termCoursesRepository.findById(tcrid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid term-course id: " + tcrid));
        Terms term = termCourse.getTerms();
        if (term == null || term.getAcademicYear() == null) {
            throw new IllegalArgumentException("Term or academic year is missing for the selected course.");
        }

        String semesterNumber = mapSemesterToNumber(term.getTrmname());
        if (semesterNumber == null) {
            throw new IllegalArgumentException("Unsupported semester name: " + term.getTrmname());
        }

        String academicYear = term.getAcademicYear().getAyrname().replaceAll("[^a-zA-Z0-9\\-]", "");
        Path directory = Paths.get(getUploadDirectory(), academicYear, semesterNumber);
        Files.createDirectories(directory);

        String fileName = courseCode.trim().replaceAll("[^a-zA-Z0-9]", "") + ".pdf";
        Path filePath = directory.resolve(fileName);
        file.transferTo(filePath.toFile());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Course PDF uploaded successfully");
        response.put("fileName", fileName);
        response.put("filePath", filePath.toString());
        response.put("courseCode", courseCode);
        response.put("semester", term.getTrmname());
        response.put("academicYear", term.getAcademicYear().getAyrname());
        return response;
    }

    private String mapSemesterToNumber(String semesterName) {
        if (semesterName == null) {
            return null;
        }
        String lower = semesterName.toLowerCase();
        if (lower.contains("autumn") || lower.contains("fall")) {
            return "01";
        }
        if (lower.contains("winter")) {
            return "02";
        }
        if (lower.contains("summer")) {
            return "03";
        }
        return null;
    }
}

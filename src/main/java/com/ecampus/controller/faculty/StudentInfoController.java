package com.ecampus.controller.faculty;

import com.ecampus.model.Students;
import com.ecampus.repository.StudentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Handles faculty requests for viewing student information and academic performance.
 */
@Controller
@RequestMapping("/faculty")
public class StudentInfoController {

    @Autowired
    private StudentsRepository studentsRepository;

    /**
     * Displays student information matching the supplied search criteria.
     *
     * @param searchType  the field to search, such as student ID or student name
     * @param searchQuery the student ID or name to search for
     * @param model       the model used to provide student data to the view
     * @return the faculty student information view name
     */
    @GetMapping("/student-info")
    public String viewStudentInfoPage(
            @RequestParam(value = "searchType", required = false, defaultValue = "studentId") String searchType,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            Model model) {
        
        List<Students> studentSuggestions = studentsRepository
                .findByStdrowstateGreaterThanOrderByStdfirstnameAscStdlastnameAsc(0);

        // Keep the typed search text visible in the search bar
        model.addAttribute("searchType", searchType);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("studentSuggestions", studentSuggestions);

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            List<Students> searchResults;
            String queryStr = searchQuery.trim();
            
            // Use the new queries
            if ("studentName".equals(searchType)) {
                searchResults = studentsRepository.getStudentInfoByName(queryStr);
            } else {
                searchResults = studentsRepository.getStudentInfoById(queryStr);
            }

            if (!searchResults.isEmpty()) {
                Students foundStudent = searchResults.get(0);
                model.addAttribute("student", foundStudent);
                
                String currentSemester = studentsRepository.getLatestSemesterForStudent(foundStudent.getStdid());
                model.addAttribute("currentSemester", currentSemester != null ? currentSemester : "N/A");
                
                // -- Fetch Academic Records (Courses + Grades)
                List<com.ecampus.dto.StudentAcademicRecordDTO> records = 
                        studentsRepository.getStudentAcademicRecords(foundStudent.getStdid());
                
                // Group records by Semester Name (LinkedHashMap preserves the chronological ORDER BY from SQL)
                java.util.Map<String, java.util.List<com.ecampus.dto.StudentAcademicRecordDTO>> recordsBySemester = new java.util.LinkedHashMap<>();
                for (com.ecampus.dto.StudentAcademicRecordDTO rec : records) {
                    recordsBySemester.computeIfAbsent(rec.getSemesterName(), k -> new java.util.ArrayList<>()).add(rec);
                }
                model.addAttribute("recordsBySemester", recordsBySemester);

                // -- Fetch Semester Performance (SPI / CPI)
                List<com.ecampus.dto.StudentSemesterResultDTO> performance = 
                        studentsRepository.getStudentSemesterResults(foundStudent.getStdid());
                
                java.util.Map<String, com.ecampus.dto.StudentSemesterResultDTO> performanceMap = new java.util.HashMap<>();
                for (com.ecampus.dto.StudentSemesterResultDTO res : performance) {
                    performanceMap.put(res.getSemesterName(), res);
                }
                model.addAttribute("performanceMap", performanceMap);
                
            } else {
                model.addAttribute("errorMessage", "No student found matching the provided criteria.");
            }
        }

        return "faculty/student-info";
    }
}
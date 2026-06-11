package com.ecampus.controller.faculty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ecampus.model.StudentGradeDTO;
import com.ecampus.model.TermCourses;
import com.ecampus.model.Terms;
import com.ecampus.model.Users;
import com.ecampus.repository.StudentRegistrationCoursesRepository;
import com.ecampus.repository.TermCoursesRepository;
import com.ecampus.repository.TermsRepository;
import com.ecampus.repository.UserRepository;
import com.ecampus.service.FileService;
import com.ecampus.service.GradeService;
import com.ecampus.session.SessionVars;

@Controller
@RequestMapping("/faculty")
public class FacultyDashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private TermCoursesRepository termCoursesRepository;

    @Autowired
    private StudentRegistrationCoursesRepository studentRegistrationCoursesRepository;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private FileService fileService;

    @Autowired
    private SessionVars sessionVars;

    @GetMapping("/dashboard")
    public String showCurrentSemesterCourses(Model model, Authentication authentication) {
        Users faculty = currentFaculty(authentication);
        if (faculty == null) {
            return "redirect:/login";
        }

        // Terms latestTerm = termsRepository.findLatestMinusThree(0);
        Terms latestTerm = termsRepository.findById(35L).orElse(null);
        if (latestTerm == null) {
            model.addAttribute("faculty", faculty);
            model.addAttribute("termCourses", List.of());
            model.addAttribute("errorMessage", "No active term found.");
            return "faculty_v2/dashboard";
        }

        List<Terms> sameYearTerms = termsRepository
                .findByAcademicYear_AyridAndTrmrowstateGreaterThanOrderByTrmnameAsc(
                        latestTerm.getAcademicYear().getAyrid(), 0);

        List<Terms> previousTermsToShow = previousTerms(latestTerm, sameYearTerms);

        Map<String, List<TermCourses>> previousCoursesByTerm = new HashMap<>();
        Map<Long, Long> previousStudentCounts = new HashMap<>();
        Map<Long, Boolean> previousGradeStatusMap = new HashMap<>();

        for (Terms previousTerm : previousTermsToShow) {
            List<TermCourses> courses = termCoursesRepository.findFacultyCoursesInTerm(
                    previousTerm.getTrmid(), faculty.getUid());

            if (!courses.isEmpty()) {
                previousCoursesByTerm.put(previousTerm.getTrmname(), courses);
            }
        }

        for (List<TermCourses> courseList : previousCoursesByTerm.values()) {
            fillStudentCounts(courseList, previousStudentCounts);
            fillGradeStatuses(courseList, previousGradeStatusMap);
        }

        List<TermCourses> termCourses = termCoursesRepository.findFacultyCoursesInTerm(
                latestTerm.getTrmid(), faculty.getUid());

        Map<Long, Long> studentCounts = new HashMap<>();
        Map<Long, Boolean> gradeStatusMap = new HashMap<>();

        fillStudentCounts(termCourses, studentCounts);
        fillGradeStatuses(termCourses, gradeStatusMap);

        model.addAttribute("faculty", faculty);
        model.addAttribute("latestTerm", latestTerm);
        model.addAttribute("termCourses", termCourses);
        model.addAttribute("studentCounts", studentCounts);
        model.addAttribute("previousTerms", previousTermsToShow);
        model.addAttribute("previousTermNames", previousTermsToShow.stream()
                .map(Terms::getTrmname)
                .collect(Collectors.joining(", ")));
        model.addAttribute("previousCoursesByTerm", previousCoursesByTerm);
        model.addAttribute("previousStudentCounts", previousStudentCounts);
        model.addAttribute("gradeStatusMap", gradeStatusMap);
        model.addAttribute("prevGradeStatusMap", previousGradeStatusMap);

        return "faculty_v2/dashboard";
    }

    @GetMapping("/grade-statistics")
    public String showGradeStatistics(@RequestParam(required = false) Long tcrid,
                                      Model model,
                                      Authentication authentication) {

        Users faculty = currentFaculty(authentication);
        if (faculty == null) {
            return "redirect:/login";
        }

        // Terms latestTerm = termsRepository.findLatestMinusThree(0);
        Terms latestTerm = termsRepository.findById(35L).orElse(null);
        if (latestTerm != null) {
            model.addAttribute("courses",
                    termCoursesRepository.findFacultyCoursesInTerm(latestTerm.getTrmid(), faculty.getUid()));
        }

        if (tcrid != null) {
            termCoursesRepository.findById(tcrid).ifPresent(tc -> {
                model.addAttribute("gradeStats",
                        gradeService.getGradeDistribution(tc.getTcrcrsid(), tc.getTcrtrmid(), 1L));
                model.addAttribute("selectedTcrid", tcrid);
                model.addAttribute("courseName", tc.getCourse().getCrsname());
            });
        }

        return "faculty_v2/grade_statistics";
    }

    @GetMapping("/registrations")
    public String showCourseRegistrations(@RequestParam Long tcrid,
                                          Model model,
                                          Authentication authentication) {

        Users faculty = currentFaculty(authentication);
        if (faculty == null) {
            return "redirect:/login";
        }

        TermCourses course = termCoursesRepository.findById(tcrid).orElse(null);
        if (course == null) {
            model.addAttribute("error", "Invalid course selected.");
            return "faculty_v2/course_registrations";
        }

        if (course.getTcrfacultyid() != null && !course.getTcrfacultyid().equals(faculty.getUid())) {
            model.addAttribute("error", "You do not have permission to view registrations for this course.");
            return "faculty_v2/course_registrations";
        }

        model.addAttribute("course", course);
        model.addAttribute("registeredStudents",
                studentRegistrationCoursesRepository.findRegisteredStudentsByTcrid(tcrid));

        return "faculty_v2/course_registrations";
    }

    @GetMapping("/get-courses")
    @ResponseBody
    public List<Map<String, Object>> getCourses(@RequestParam String year,
                                                @RequestParam String termName,
                                                Authentication authentication) {

        Users faculty = currentFaculty(authentication);
        if (faculty == null) {
            return List.of();
        }

        Terms term = termsRepository.findByYearAndTerm(year, termName);
        if (term == null) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (TermCourses tc : termCoursesRepository.findFacultyCoursesInTerm(term.getTrmid(), faculty.getUid())) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", tc.getTcrid());
            map.put("name", tc.getCourse().getCrsname() + " (" + tc.getCourse().getCrscode() + ")");
            result.add(map);
        }

        return result;
    }

    @GetMapping("/past-courses")
    public String pastCoursesForm(Authentication authentication, Model model) {
        if (currentFaculty(authentication) == null) {
            return "redirect:/login";
        }

        model.addAttribute("academicYears", termsRepository.findAllAcademicYears());
        return "faculty_v2/past_courses_form";
    }

    @PostMapping("/past-courses")
    public String pastCoursesResult(@RequestParam Long tcrid,
                                    @RequestParam String termName,
                                    @RequestParam String year,
                                    Model model) {

        TermCourses tc = termCoursesRepository.findById(tcrid).orElse(null);
        if (tc == null) {
            model.addAttribute("error", "Invalid Course");
            return "faculty_v2/past_courses_form";
        }

        List<Object[]> students = new ArrayList<>();

        for (StudentGradeDTO grade : gradeService.getStudentGrades(
                tc.getTcrcrsid(),
                tc.getTcrtrmid(),
                1L,
                List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F", "I", "NULL"))) {

            students.add(new Object[]{
                    grade.getStudentId(),
                    grade.getStudentName(),
                    grade.getGrade(),
                    "-"
            });
        }

        model.addAttribute("students", students);
        model.addAttribute("courseName", tc.getCourse().getCrsname());
        model.addAttribute("termName", termName);
        model.addAttribute("year", year);

        return "faculty_v2/past_courses";
    }

    @GetMapping("/upload-course")
    public String uploadCourseForm(Model model, Authentication authentication) {
        Users faculty = currentFaculty(authentication);
        if (faculty == null) {
            return "redirect:/login";
        }

        // Terms latestTerm = termsRepository.findLatestMinusThree(0);
        Terms latestTerm = termsRepository.findById(35L).orElse(null);
        if (latestTerm == null) {
            model.addAttribute("errorMessage", "No active term found.");
            return "faculty_v2/upload_course";
        }

        model.addAttribute("courses",
                termCoursesRepository.findFacultyCoursesInTerm(latestTerm.getTrmid(), faculty.getUid()));
        model.addAttribute("currentTerm", latestTerm);
        model.addAttribute("academicYear", latestTerm.getAcademicYear());

        return "faculty_v2/upload_course";
    }

    @GetMapping("/uploadCourseFile")
    public String uploadCourseFileForm() {
        return "redirect:/faculty/upload-course";
    }

    @PostMapping("/upload-course")
    @ResponseBody
    public Map<String, Object> uploadCourse(@RequestParam("file") MultipartFile file,
                                            @RequestParam Long tcrid,
                                            @RequestParam String courseCode,
                                            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        Users faculty = currentFaculty(authentication);
        if (faculty == null) {
            response.put("success", false);
            response.put("message", "Session expired. Please login again.");
            return response;
        }

        try {
            TermCourses tc = termCoursesRepository.findById(tcrid).orElse(null);

            if (tc == null || (tc.getTcrfacultyid() != null && !tc.getTcrfacultyid().equals(faculty.getUid()))) {
                response.put("success", false);
                response.put("message", "You do not have permission to upload for this course.");
                return response;
            }

            return fileService.uploadCoursePdf(file, tcrid, courseCode);

        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return response;
        }
    }

    private Users currentFaculty(Authentication authentication) {

        Users user = sessionVars == null ? null : sessionVars.getLoggedInUser();

        if (user == null && authentication != null &&
                authentication.getName() != null &&
                !authentication.getName().isBlank()) {

            user = userRepository.findWithName(authentication.getName()).orElse(null);
        }

        if (user == null) {
            return null;
        }

        String role = user.getrole();

        if (!"FACULTY".equalsIgnoreCase(role)) {
            return null;
        }

        return user;
    }

    private List<Terms> previousTerms(Terms latestTerm, List<Terms> sameYearTerms) {
        List<Terms> previousTerms = new ArrayList<>();
        String current = latestTerm.getTrmname();

        for (Terms term : sameYearTerms) {
            String name = term.getTrmname();

            if ("Autumn".equalsIgnoreCase(current)
                    && ("Summer".equalsIgnoreCase(name) || "Winter".equalsIgnoreCase(name))) {
                previousTerms.add(term);
            } else if ("Summer".equalsIgnoreCase(current)
                    && ("Autumn".equalsIgnoreCase(name) || "Winter".equalsIgnoreCase(name))) {
                previousTerms.add(term);
            } else if ("Winter".equalsIgnoreCase(current) && "Autumn".equalsIgnoreCase(name)) {
                previousTerms.add(term);
            }
        }

        return previousTerms;
    }

    private void fillStudentCounts(List<TermCourses> courses, Map<Long, Long> target) {
        if (courses == null || courses.isEmpty()) {
            return;
        }

        List<Long> tcrIds = courses.stream()
                .map(TermCourses::getTcrid)
                .toList();

        for (Object[] row : termCoursesRepository.countStudentsForTermCourses(tcrIds)) {
            target.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
    }

    private void fillGradeStatuses(List<TermCourses> courses, Map<Long, Boolean> target) {
        if (courses == null) {
            return;
        }

        for (TermCourses tc : courses) {
            target.put(tc.getTcrid(),
                    gradeService.checkIfGradesExist(tc.getTcrcrsid(), tc.getTcrtrmid(), 1L));
        }
    }
}

package com.ecampus.controller.student;

import com.ecampus.config.RegistrationDeadlineConfig;
import com.ecampus.dto.CourseRegistrationDTO;
import com.ecampus.model.*;
import com.ecampus.repository.UserRepository;
import com.ecampus.session.SessionVars;
import com.ecampus.service.*;
import com.ecampus.util.RomanNumeralUtil;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class StudentRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(StudentRegistrationController.class);

    @Value("${registration.testing.override-enabled:false}")
    private boolean testOverrideEnabled;

    @Value("${registration.testing.editable-strids:}")
    private String testEditableStrids;

    @Value("${registration.testing.backlog-override-enabled:false}")
    private boolean testBacklogOverrideEnabled;

    @Value("${registration.testing.backlog-override-stdids:}")
    private String testBacklogOverrideStdids;

    @Value("${registration.testing.backlog-override-stdinstids:}")
    private String testBacklogOverrideStdinstids;

    @Autowired
    private StudentRegistrationService registrationService;

    @Autowired
    private RegistrationDeadlineConfig deadlineConfig;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private SessionVars sessionVars;

    @GetMapping("/student/registration")
    public String listStudentRegistrations( Authentication authentication, Model model) {
        Long studentId = resolveStudentId(authentication);
        if (studentId == null) {
            model.addAttribute("error", "Unable to resolve student account");
            return "student/student-registration";
        }

        Students st = registrationService.getStudentById(studentId);

        List<Semesters> smt = registrationService.getSemesterById(st.getStdbchid());

        Map<Long, String> semesterDisplayMap = smt.stream()
            .collect(Collectors.toMap(
                Semesters::getStrid,
                sem -> {
                    Long seq = sem.getStrseqno();
                    if (seq != null && seq > 0 && seq <= 3999) {
                    return "Semester " + RomanNumeralUtil.toRoman(seq);
                    }
                    return sem.getStrname();
                },
                (a, b) -> a,
                LinkedHashMap::new
            ));

        List<StudentRegistrations> regs = registrationService.getRegistrationsByStudentId(studentId);

        Map<Long, StudentRegistrations> registrationsMap = regs.stream()
                .collect(Collectors.toMap(StudentRegistrations::getSrgstrid, Function.identity()));

        boolean isWithinDeadline = deadlineConfig.isWithinDeadline();

        Long maxStrid = getLatestRegisteredSemesterId(smt, regs);
        if (maxStrid == null) {
            maxStrid = registrationService.getMaxSemesterId(st.getStdbchid());
        }

        Set<Long> editableSemesterIds = new HashSet<>();
        if (maxStrid != null && isWithinDeadline) {
            editableSemesterIds.add(maxStrid);
        }
        editableSemesterIds.addAll(getTestingEditableSemesterIds());

        model.addAttribute("isWithinDeadline", isWithinDeadline);
        model.addAttribute("maxStrid", maxStrid);
        model.addAttribute("studentbean", st);
        model.addAttribute("semestersbean", smt);
        model.addAttribute("editableSemesterIds", editableSemesterIds);
        model.addAttribute("semesterDisplayMap", semesterDisplayMap);
        model.addAttribute("StudentRegistrations",registrationsMap);
        return "student/student-registration";
    }
    
    /**
     * Display registration edit form for a specific semester
     */
    @GetMapping("/student/registration/edit")
    public String editRegistration(
            @RequestParam Long strid,
            Authentication authentication,
            Model model) {
        Long studentId = resolveStudentId(authentication);
        if (studentId == null) {
            model.addAttribute("error", "Unable to resolve student account");
            return "redirect:/student/registration";
        }
        
        Students student = registrationService.getStudentById(studentId);
        
        // Get semester details
        Semesters semester = registrationService.getSemesterById(student.getStdbchid())
                .stream()
                .filter(s -> s.getStrid().equals(strid))
                .findFirst()
                .orElse(null);
        
        if (semester == null) {
            model.addAttribute("error", "Semester not found");
            return "redirect:/student/registration";
        }
        
        // Check if editable (latest registered semester and within deadline)
        List<StudentRegistrations> regs = registrationService.getRegistrationsByStudentId(studentId);
        Long editableStrid = getLatestRegisteredSemesterId(registrationService.getSemesterById(student.getStdbchid()), regs);
        if (editableStrid == null) {
            editableStrid = registrationService.getMaxSemesterId(student.getStdbchid());
        }
        boolean isEditable = (editableStrid != null && editableStrid.equals(strid) && deadlineConfig.isWithinDeadline())
            || getTestingEditableSemesterIds().contains(strid);
        
        // Get CORE courses for this semester
        List<CourseRegistrationDTO> coreCourses = registrationService.getCoreCoursesBySemester(strid);
        List<CourseRegistrationDTO> additionalCourses = List.of();
        
        // Check for existing registration
        StudentRegistrations existingReg = registrationService.getExistingRegistration(studentId, strid);
        
        // If editing existing registration, populate with saved data
        if (existingReg != null) {
            List<StudentRegistrationCourses> registeredCourses = registrationService.getRegistrationCourses(existingReg.getSrgid());
            
            // Merge existing registration data with courses
            for (CourseRegistrationDTO courseDTO : coreCourses) {
                registeredCourses.stream()
                    .filter(rc -> Objects.equals(rc.getSrcscrid(), courseDTO.getScrid()))
                    .findFirst()
                    .ifPresent(rc -> {
                        courseDTO.setSrcid(rc.getSrcid());
                        courseDTO.setRegType(rc.getSrctype());
                        courseDTO.setRemarks(rc.getSrcfield1());
                    });
            }

            additionalCourses = registrationService.getAdditionalRegisteredCourses(existingReg.getSrgid(), strid, studentId);
        }
        
        model.addAttribute("studentbean", student);
        model.addAttribute("semesterbean", semester);
        model.addAttribute("coreCourses", coreCourses);
        model.addAttribute("additionalCourses", additionalCourses);
        model.addAttribute("editable", isEditable);
        model.addAttribute("selectedStrid", strid);
        model.addAttribute("registrationHeader", existingReg);
        model.addAttribute("currentDate", java.time.LocalDate.now());
        
        return "student/student-registration-edit";
    }

    @GetMapping("/student/registration/additional")
    public String additionalCoursesPage(
            @RequestParam Long strid,
            Authentication authentication,
            Model model) {
        Long studentId = resolveStudentId(authentication);
        if (studentId == null) {
            model.addAttribute("error", "Unable to resolve student account");
            return "redirect:/student/registration";
        }

        Students student = registrationService.getStudentById(studentId);
        Semesters semester = registrationService.getSemesterById(student.getStdbchid())
                .stream()
                .filter(s -> s.getStrid().equals(strid))
                .findFirst()
                .orElse(null);
        if (semester == null) {
            model.addAttribute("error", "Semester not found");
            return "redirect:/student/registration";
        }

        List<CourseRegistrationDTO> backlogCourses = registrationService
            .getBacklogCoursesForStudentBySemester(studentId, strid);
        log.info("Additional courses load: username={}, resolvedStudentId={}, semesterId={}, backlogCount={}",
                authentication.getName(), studentId, strid, backlogCourses.size());
        if (backlogCourses.isEmpty() && shouldUseBacklogTestingOverride(student)) {
            backlogCourses = registrationService.getCoreCoursesBySemester(strid).stream()
                .map(course -> {
                course.setRegType("BACKLOG");
                course.setIsDrop(false);
                return course;
                })
                .collect(Collectors.toList());
        }
        List<CourseRegistrationDTO> gradeImprovementCourses = registrationService
            .getGradeImprovementCoursesForStudentBySemester(studentId, strid);
        List<CourseRegistrationDTO> projectInternships = registrationService.getProjectInternshipCoursesBySemester(strid);
            List<CourseRegistrationDTO> coreCourses = registrationService.getCoreCoursesBySemester(strid);
            List<CourseRegistrationDTO> preselectedAdditional = new ArrayList<>();

        StudentRegistrations existingReg = registrationService.getExistingRegistration(studentId, strid);
        if (existingReg != null) {
                preselectedAdditional = registrationService
                    .getAdditionalRegisteredCourses(
                        existingReg.getSrgid(),
                        strid,
                        studentId,
                        coreCourses,
                        backlogCourses,
                        gradeImprovementCourses,
                        projectInternships);
                Map<Long, CourseRegistrationDTO> byScrid = preselectedAdditional.stream()
                    .collect(Collectors.toMap(CourseRegistrationDTO::getScrid, Function.identity(), (a, b) -> a));

            for (CourseRegistrationDTO row : backlogCourses) {
                CourseRegistrationDTO reg = byScrid.get(row.getScrid());
                if (reg != null) {
                    row.setSrcid(reg.getSrcid());
                    row.setRegType(reg.getRegType());
                    row.setRemarks(reg.getRemarks());
                    row.setIsDrop(false);
                }
            }

            for (CourseRegistrationDTO row : projectInternships) {
                CourseRegistrationDTO reg = byScrid.get(row.getScrid());
                if (reg != null) {
                    row.setSrcid(reg.getSrcid());
                    row.setRegType(reg.getRegType());
                    row.setRemarks(reg.getRemarks());
                    row.setIsDrop(false);
                }
            }

            for (CourseRegistrationDTO row : gradeImprovementCourses) {
                CourseRegistrationDTO reg = byScrid.get(row.getScrid());
                if (reg != null) {
                    row.setSrcid(reg.getSrcid());
                    row.setRegType(reg.getRegType());
                    row.setRemarks(reg.getRemarks());
                    row.setIsDrop(false);
                }
            }
        }

        List<CourseRegistrationDTO> auditCourses = registrationService.getAuditCoursesForStudentBySemester(
                studentId,
                strid,
                coreCourses,
                backlogCourses,
                gradeImprovementCourses,
                projectInternships,
                preselectedAdditional);

        if (!preselectedAdditional.isEmpty()) {
            Set<Long> auditScrids = auditCourses.stream()
                    .map(CourseRegistrationDTO::getScrid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (CourseRegistrationDTO registered : preselectedAdditional) {
                if (!"AUDIT".equalsIgnoreCase(registered.getRegType())) {
                    continue;
                }
                if (registered.getScrid() == null || auditScrids.contains(registered.getScrid())) {
                    continue;
                }
                registered.setIsDrop(false);
                auditCourses.add(registered);
                auditScrids.add(registered.getScrid());
            }
        }

        model.addAttribute("studentbean", student);
        model.addAttribute("semesterbean", semester);
        model.addAttribute("selectedStrid", strid);
        model.addAttribute("backlogCourses", backlogCourses);
        model.addAttribute("gradeImprovementCourses", gradeImprovementCourses);
        model.addAttribute("projectInternshipCourses", projectInternships);
        model.addAttribute("auditCourses", auditCourses);
        return "student/student-registration-additional";
    }

    @PostMapping("/student/registration/additional/save")
    public String saveAdditionalCourses(
            @RequestParam Long strid,
            @RequestParam(defaultValue = "") Map<String, String> allParams,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Long studentId = resolveStudentId(authentication);
        if (studentId == null) {
            redirectAttributes.addFlashAttribute("error", "Unable to resolve student account");
            return "redirect:/student/registration";
        }

        String username = authentication.getName();
        Long userId = userRepo.findUidByUname(username);
        if (userId == null) {
            userId = 0L;
        }

        List<CourseRegistrationDTO> additionalCourses = parseCoursesFromRequest(allParams);
        List<CourseRegistrationDTO> missingTermCourse = additionalCourses.stream()
            .filter(dto -> !Boolean.TRUE.equals(dto.getIsDrop()))
            .filter(dto -> dto.getScrid() == null || dto.getScrtcrid() == null)
            .collect(Collectors.toList());
        if (!missingTermCourse.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                "One or more selected courses are missing term-course mapping. Please reselect and submit again.");
            return "redirect:/student/registration/additional?strid=" + strid;
        }
        registrationService.saveAdditionalCourses(studentId, strid, additionalCourses, userId);

        redirectAttributes.addFlashAttribute("success", "Additional courses updated successfully.");
        return "redirect:/student/registration/edit?strid=" + strid;
    }
    
    /**
     * Save student registration and courses
     */
    @PostMapping("/student/registration/save")
    public String saveRegistration(
            @RequestParam Long strid,
            @RequestParam(value = "rows[0].scrid", required = false) Long scrid0,
            @RequestParam(defaultValue = "") Map<String, String> allParams,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        Long studentId = resolveStudentId(authentication);
        if (studentId == null) {
            redirectAttributes.addFlashAttribute("error", "Unable to resolve student account");
            return "redirect:/student/registration";
        }

        Long userId = userRepo.findUidByUname(username);
        if (userId == null) {
            userId = 0L;
        }
        
        // Parse courses from form submission
        List<CourseRegistrationDTO> submitteeCourses = parseCoursesFromRequest(allParams);
        
        try {
            // Save registration
            StudentRegistrations savedReg = registrationService.saveStudentRegistration(
                studentId, strid, submitteeCourses, userId);
            
            redirectAttributes.addFlashAttribute("success", "Registration saved successfully for semester!");
            return "redirect:/student/registration";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error saving registration: " + e.getMessage());
            return "redirect:/student/registration/edit?strid=" + strid;
        }
    }
    
    /**
     * Parse courses from form request parameters
     */
    private List<CourseRegistrationDTO> parseCoursesFromRequest(Map<String, String> allParams) {
        List<CourseRegistrationDTO> courses = new ArrayList<>();
        
        // Pattern: rows[0].scrid, rows[0].tcrid, rows[0].regType, etc.
        Map<Integer, CourseRegistrationDTO> courseMap = new TreeMap<>();
        
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Parse index from key like "rows[0].scrid"
            if (key.startsWith("rows[")) {
                int endIndex = key.indexOf("]");
                if (endIndex > 0) {
                    try {
                        int idx = Integer.parseInt(key.substring(5, endIndex));
                        String fieldName = key.substring(endIndex + 2);
                        
                        CourseRegistrationDTO course = courseMap.computeIfAbsent(idx, k -> new CourseRegistrationDTO());
                        
                        switch (fieldName) {
                            case "srcid":
                                course.setSrcid(Long.parseLong(value));
                                break;
                            case "scrid":
                                course.setScrid(Long.parseLong(value));
                                break;
                            case "tcrid":
                                course.setScrtcrid(Long.parseLong(value));
                                break;
                            case "regType":
                                course.setRegType(value);
                                break;
                            case "remarks":
                                course.setRemarks(value);
                                break;
                            case "drop":
                                course.setIsDrop(value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true"));
                                break;
                        }
                    } catch (NumberFormatException e) {
                        // Skip parsing errors
                    }
                }
            }
        }
        
        courses.addAll(courseMap.values().stream()
            .filter(course -> course.getScrid() != null || course.getSrcid() != null)
            .collect(Collectors.toList()));
        return courses;
    }

    private Long resolveStudentId(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        Users user = sessionVars == null ? null : sessionVars.getLoggedInUser();
        if (user == null && username != null) {
            user = userRepo.findByUname(username).orElse(null);
        }
        if (user == null) {
            return null;
        }

        Long studentId = user.getStdid();
        String instituteId = user.getUnivId();
        if (instituteId != null && !instituteId.isBlank()) {
            Long latestStudentId = registrationService.getLatestStudentIdByInstituteId(instituteId);
            if (latestStudentId != null) {
                studentId = latestStudentId;
            }
        }

        // Student usernames are often institute IDs; use them as a fallback resolver.
        if (username != null && username.matches("\\d+")) {
            Long mappedByUsername = registrationService.getLatestStudentIdByInstituteId(username);
            if (mappedByUsername != null) {
                studentId = mappedByUsername;
            }
        }

        return studentId;
    }

    private Long getLatestRegisteredSemesterId(List<Semesters> semesters, List<StudentRegistrations> regs) {
        if (semesters == null || semesters.isEmpty() || regs == null || regs.isEmpty()) {
            return null;
        }

        Map<Long, Long> seqBySemesterId = semesters.stream()
                .collect(Collectors.toMap(Semesters::getStrid, s -> s.getStrseqno() == null ? 0L : s.getStrseqno(), (a, b) -> a));

        return regs.stream()
                .map(StudentRegistrations::getSrgstrid)
                .filter(seqBySemesterId::containsKey)
                .max(Comparator.comparingLong(id -> seqBySemesterId.getOrDefault(id, 0L)))
                .orElse(null);
    }

    private Set<Long> getTestingEditableSemesterIds() {
        if (!testOverrideEnabled) {
            return Set.of();
        }

        return parseLongCsv(testEditableStrids);
    }

    private boolean shouldUseBacklogTestingOverride(Students student) {
        if (!testBacklogOverrideEnabled || student == null) {
            return false;
        }

        Set<Long> stdids = parseLongCsv(testBacklogOverrideStdids);
        Set<String> stdinstids = parseStringCsv(testBacklogOverrideStdinstids);

        boolean byStdid = student.getStdid() != null && stdids.contains(student.getStdid());
        boolean byStdinstid = student.getStdinstid() != null && stdinstids.contains(student.getStdinstid().trim());
        return byStdid || byStdinstid;
    }

    private Set<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (String token : csv.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
                // Ignore malformed test IDs.
            }
        }
        return ids;
    }

    private Set<String> parseStringCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }

        Set<String> values = new HashSet<>();
        for (String token : csv.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }
}

package com.ecampus.controller.faculty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecampus.model.Courses;
import com.ecampus.model.Grade;
import com.ecampus.model.GradeChangeStatusDTO;
import com.ecampus.model.StudentGradeDTO;
import com.ecampus.model.StudentGradeDTOWrapper;
import com.ecampus.model.TermCourses;
import com.ecampus.repository.TermCoursesRepository;
import com.ecampus.service.FileService;
import com.ecampus.service.GradeService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/grades")
@SessionAttributes("gradeForm")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private FileService fileService;

    @Autowired
    private TermCoursesRepository termCourseRepository;

    @GetMapping("/view")
    public String viewGrades(@RequestParam Long tcrid, HttpSession session, ModelMap model) {
        TermCourses tc = termCourseRepository.findById(tcrid).orElse(null);
        if (tc == null) {
            return "redirect:/faculty/dashboard";
        }

        Long crsid = tc.getTcrcrsid();
        Long trmid = tc.getTcrtrmid();
        session.setAttribute("CRSID", crsid);
        session.setAttribute("TRMID", trmid);
        session.setAttribute("examTypeId", 1L);

        Courses course = tc.getCourse();
        model.addAttribute("studentGrades", gradeService.getStudentGrades(crsid, trmid, 1L,
                List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F", "I", "NULL")));
        model.addAttribute("CourseName", gradeService.getCourseName(crsid));
        model.addAttribute("CourseCode", course == null ? "" : course.getCrscode());
        model.addAttribute("TermName", gradeService.getTermName(trmid));
        return "faculty_v2/view_grade";
    }

    @GetMapping("/upload-options")
    public String showUploadOptions(@RequestParam Long tcrid, ModelMap model) {
        model.addAttribute("tcrid", tcrid);
        return "faculty_v2/upload_options";
    }

    @GetMapping("/upload")
    public String prepareUpload(@RequestParam Long tcrid, HttpSession session) {
        TermCourses tc = termCourseRepository.findById(tcrid).orElse(null);
        if (tc == null) {
            return "redirect:/faculty/dashboard";
        }
        session.setAttribute("CRSID", tc.getTcrcrsid());
        session.setAttribute("TRMID", tc.getTcrtrmid());
        session.setAttribute("examTypeId", 1L);
        return "redirect:/grades/upload/form";
    }

    @GetMapping("/upload/form")
    public String showUploadForm(ModelMap model, HttpSession session) {
        Long crsid = (Long) session.getAttribute("CRSID");
        Long trmid = (Long) session.getAttribute("TRMID");
        Long examTypeId = (Long) session.getAttribute("examTypeId");
        if (crsid == null || trmid == null || examTypeId == null) {
            return "redirect:/faculty/dashboard";
        }

        StudentGradeDTOWrapper gradeForm = new StudentGradeDTOWrapper();
        gradeForm.setGradesList(gradeService.getStudentGrades(crsid, trmid, examTypeId,
                List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F", "I", "NULL")));
        model.addAttribute("TermName", gradeService.getTermName(trmid));
        model.addAttribute("CourseName", gradeService.getCourseName(crsid));
        model.addAttribute("grades", gradeService.getAllGrades());
        model.addAttribute("gradeForm", gradeForm);
        return "faculty_v2/upload_grade";
    }

    @PostMapping("/upload")
    public String saveGrades(@ModelAttribute("gradeForm") StudentGradeDTOWrapper gradeWrapper,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Long tcrid = currentTcrid(session);
        Long examTypeId = (Long) session.getAttribute("examTypeId");
        if (tcrid == null || examTypeId == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired or incomplete context.");
            return "redirect:/faculty/dashboard";
        }

        List<StudentGradeDTO> gradesToProcess = gradeWrapper.getGradesList().stream()
                .filter(dto -> dto.getModifiedGrade() != null && !dto.getModifiedGrade().isBlank())
                .collect(Collectors.toList());
        if (gradesToProcess.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "No grades were entered.");
            return "redirect:/grades/upload/form";
        }

        gradeService.saveOrUpdateGrades(gradesToProcess, tcrid, examTypeId);
        redirectAttributes.addFlashAttribute("success", "Grades updated successfully.");
        return "redirect:/faculty/dashboard";
    }

    @PostMapping("/uploadcsv")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewCsv(@RequestParam("file") MultipartFile file) throws Exception {
        Map<String, Object> response = new HashMap<>();
        if (!fileService.hasCsvFormat(file)) {
            response.put("message", "Please upload a CSV file.");
            return ResponseEntity.badRequest().body(response);
        }

        List<StudentGradeDTO> parsed = fileService.csvToStudentGradeDTOs(file.getInputStream());
        response.put("message", "CSV parsed successfully.");
        response.put("gradesList", parsed);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/savecsv")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveCsv(@RequestBody List<StudentGradeDTO> grades, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Long tcrid = currentTcrid(session);
        Long examTypeId = (Long) session.getAttribute("examTypeId");
        if (tcrid == null || examTypeId == null) {
            response.put("message", "Session expired or incomplete context.");
            return ResponseEntity.badRequest().body(response);
        }

        gradeService.saveOrUpdateGrades(grades, tcrid, examTypeId);
        response.put("message", "Grades saved successfully.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/update")
    public String prepareUpdate(@RequestParam Long tcrid, HttpSession session) {
        TermCourses tc = termCourseRepository.findById(tcrid).orElse(null);
        if (tc == null) {
            return "redirect:/faculty/dashboard";
        }
        session.setAttribute("CRSID", tc.getTcrcrsid());
        session.setAttribute("TRMID", tc.getTcrtrmid());
        session.setAttribute("examTypeId", 1L);
        return "redirect:/grades/update/form";
    }

    @GetMapping("/update/form")
    public String showUpdateForm(ModelMap model, HttpSession session) {
        Long crsid = (Long) session.getAttribute("CRSID");
        Long trmid = (Long) session.getAttribute("TRMID");
        Long examTypeId = (Long) session.getAttribute("examTypeId");
        if (crsid == null || trmid == null || examTypeId == null) {
            return "redirect:/faculty/dashboard";
        }

        List<StudentGradeDTO> studentGrades = gradeService.getStudentGrades(crsid, trmid, examTypeId,
                List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F")).stream()
                .filter(dto -> dto.getGrade() != null && !"NULL".equalsIgnoreCase(dto.getGrade()))
                .collect(Collectors.toList());

        StudentGradeDTOWrapper gradeForm = new StudentGradeDTOWrapper();
        gradeForm.setGradesList(studentGrades);
        model.addAttribute("TermName", gradeService.getTermName(trmid));
        model.addAttribute("CourseName", gradeService.getCourseName(crsid));
        model.addAttribute("grades", gradeService.getAllGrades());
        model.addAttribute("gradeForm", gradeForm);
        return "faculty_v2/modify_grade";
    }

    @GetMapping("/chart-data")
    @ResponseBody
    public ResponseEntity<List<StudentGradeDTO>> getChartData(HttpSession session) {
        Long crsid = (Long) session.getAttribute("CRSID");
        Long trmid = (Long) session.getAttribute("TRMID");
        Long examTypeId = (Long) session.getAttribute("examTypeId");
        if (crsid == null || trmid == null || examTypeId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(gradeService.getStudentGrades(crsid, trmid, examTypeId,
                List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F")));
    }

    @GetMapping("/reviseIGrade")
    public String prepareRevise(@RequestParam Long tcrid, HttpSession session) {
        return prepareUpdate(tcrid, session).replace("/update/form", "/reviseIGrade/form");
    }

    @GetMapping("/reviseIGrade/form")
    public String showReviseForm(ModelMap model, HttpSession session) {
        Long crsid = (Long) session.getAttribute("CRSID");
        Long trmid = (Long) session.getAttribute("TRMID");
        Long examTypeId = (Long) session.getAttribute("examTypeId");
        if (crsid == null || trmid == null || examTypeId == null) {
            return "redirect:/faculty/dashboard";
        }

        StudentGradeDTOWrapper gradeForm = new StudentGradeDTOWrapper();
        gradeForm.setGradesList(gradeService.getStudentGrades(crsid, trmid, examTypeId, List.of("I")).stream()
                .filter(dto -> "I".equalsIgnoreCase(dto.getGrade()))
                .collect(Collectors.toList()));
        model.addAttribute("TermName", gradeService.getTermName(trmid));
        model.addAttribute("CourseName", gradeService.getCourseName(crsid));
        model.addAttribute("grades", gradeService.getAllGrades());
        model.addAttribute("gradeForm", gradeForm);
        return "faculty_v2/revise_IGrade";
    }

    @PostMapping("/reviseIGrade")
    public String saveReviseGrade(@ModelAttribute("gradeForm") StudentGradeDTOWrapper gradeWrapper,
            HttpSession session, RedirectAttributes redirectAttributes) {
        return saveGrades(gradeWrapper, session, redirectAttributes);
    }

    @GetMapping("/approval/status")
    public String viewStatus(@RequestParam(name = "tcrid", required = false) Long tcrid,
            ModelMap model, HttpSession session) {
        if (tcrid != null) {
            termCourseRepository.findById(tcrid).ifPresent(tc -> {
                session.setAttribute("CRSID", tc.getTcrcrsid());
                session.setAttribute("TRMID", tc.getTcrtrmid());
            });
        }

        Long crsid = (Long) session.getAttribute("CRSID");
        Long trmid = (Long) session.getAttribute("TRMID");
        Long facultyId = (Long) session.getAttribute("userid");
        if (crsid == null || trmid == null || facultyId == null) {
            return "redirect:/faculty/dashboard";
        }

        Long resolvedTcrid = tcrid == null ? termCourseRepository.findTcridByCrsidAndTrmid(crsid, trmid) : tcrid;
        List<GradeChangeStatusDTO> statuses = resolvedTcrid == null ? new ArrayList<>()
                : gradeService.getGradeChangeStatuses(facultyId, facultyId.toString(), resolvedTcrid);

        model.addAttribute("statuses", statuses);
        model.addAttribute("courseName", gradeService.getCourseName(crsid));
        model.addAttribute("termName", gradeService.getTermName(trmid));
        model.addAttribute("isDean", false);
        model.addAttribute("isRegistrar", false);
        return "faculty_v2/grade_approval";
    }

    @GetMapping("/approval/dean")
    public String viewPendingDeanRequests(ModelMap model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userid");
        if (userId == null || !userId.equals(1150L)) {
            return "redirect:/accessDenied";
        }
        model.addAttribute("statuses", gradeService.getPendingDeanRequests());
        model.addAttribute("courseName", "Pending Dean Approvals");
        model.addAttribute("termName", "");
        model.addAttribute("isDean", true);
        model.addAttribute("isRegistrar", false);
        return "faculty_v2/grade_approval";
    }

    @GetMapping("/approval/registrar")
    public String viewPendingRegistrarRequests(ModelMap model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userid");
        if (userId == null || !userId.equals(20L)) {
            return "redirect:/accessDenied";
        }
        model.addAttribute("statuses", gradeService.getPendingRegistrarRequests());
        model.addAttribute("courseName", "Pending Registrar Approvals");
        model.addAttribute("termName", "");
        model.addAttribute("isDean", false);
        model.addAttribute("isRegistrar", true);
        return "faculty_v2/grade_approval";
    }

    @PostMapping("/approval/dean/action")
    public String deanAction(@RequestParam Long gmdid, @RequestParam String action,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Long deanId = (Long) session.getAttribute("userid");
        if (deanId == null || !deanId.equals(1150L)) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized access");
            return "redirect:/accessDenied";
        }
        gradeService.processDeanAction(gmdid, deanId, action);
        return "redirect:/grades/approval/dean";
    }

    @PostMapping("/approval/registrar/action")
    public String registrarAction(@RequestParam Long gmdid, @RequestParam String action,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Long registrarId = (Long) session.getAttribute("userid");
        if (registrarId == null || !registrarId.equals(20L)) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized access");
            return "redirect:/accessDenied";
        }
        gradeService.processRegistrarAction(gmdid, registrarId, action);
        return "redirect:/grades/approval/registrar";
    }

    private Long currentTcrid(HttpSession session) {
        Long crsid = (Long) session.getAttribute("CRSID");
        Long trmid = (Long) session.getAttribute("TRMID");
        if (crsid == null || trmid == null) {
            return null;
        }
        return termCourseRepository.findTcridByCrsidAndTrmid(crsid, trmid);
    }
}

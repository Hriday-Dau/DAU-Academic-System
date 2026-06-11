package com.ecampus.controller.faculty;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecampus.model.StudentGradeDTO;
import com.ecampus.model.StudentGradeDTOWrapper;
import com.ecampus.repository.TermCoursesRepository;
import com.ecampus.service.GradeService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/grades")
public class GradeModRequestController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private TermCoursesRepository termCourseRepository;

    @InitBinder("gradeForm")
    public void initBinder(WebDataBinder binder) {
        binder.setAutoGrowNestedPaths(true);
        binder.setAutoGrowCollectionLimit(1024);
    }

    @PostMapping("/request")
    public String submitGradeModificationRequest(
            @ModelAttribute("gradeForm") StudentGradeDTOWrapper gradeWrapper,
            @RequestParam(name = "deanRemark", required = false) String deanRemark,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long uid = (Long) session.getAttribute("userid");
        Long trmid = (Long) session.getAttribute("TRMID");
        Long crsid = (Long) session.getAttribute("CRSID");
        Long examTypeId = (Long) session.getAttribute("examTypeId");

        if (uid == null || trmid == null || crsid == null || examTypeId == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired or incomplete context.");
            return "redirect:/faculty/dashboard";
        }

        Long tcrid = termCourseRepository.findTcridByCrsidAndTrmid(crsid, trmid);
        if (tcrid == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid term-course combination.");
            return "redirect:/faculty/dashboard";
        }

        List<StudentGradeDTO> gradesToProcess = gradeWrapper.getGradesList().stream()
                .filter(StudentGradeDTO::isSelectedForUpdate)
                .collect(Collectors.toList());

        if (gradesToProcess.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "No students were selected for grade modification.");
            return "redirect:/grades/update/form";
        }

        try {
            Long requestId = gradeService.createGradeModificationRequest(uid, tcrid, examTypeId, deanRemark,
                    gradesToProcess);
            redirectAttributes.addFlashAttribute("success",
                    "Grade modification request created successfully (Request ID: " + requestId + ").");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not create grade modification request: " + ex.getMessage());
            return "redirect:/grades/update/form";
        }

        return "redirect:/grades/approval/status";
    }
}

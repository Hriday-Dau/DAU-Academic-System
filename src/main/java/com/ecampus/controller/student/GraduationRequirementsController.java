package com.ecampus.controller.student;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import com.ecampus.dto.CourseConversionTypeGroupDTO;
import com.ecampus.dto.CourseTypeProgressDTO;
import com.ecampus.dto.OverallCourseTypeProgressDTO;
import com.ecampus.model.Batches;
import com.ecampus.model.Students;
import com.ecampus.session.SessionConstants;
import com.ecampus.service.StudentGraduationRequirementsService;
import com.ecampus.util.LoggedUser;
import com.ecampus.util.UnAuthorisedUserException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student/graduation-requirements")
public class GraduationRequirementsController {

    @Autowired
    private StudentGraduationRequirementsService srgService;

    @GetMapping("/current-semester")
        public String CurrentSemester(HttpSession session, Model model) {
                StudentContext context = buildStudentContext(session, model);

        List<CourseTypeProgressDTO> progressList = srgService
                .buildCurrentSemesterProgress(context.stdid(), context.batchId(),
                        context.schemeId(), context.splid(), context.currentSemester());

        // Compute summary stats
        long totalExtra = progressList.stream().mapToLong(CourseTypeProgressDTO::getExtraCount).sum();
        long totalCompleted = progressList.stream().mapToLong(CourseTypeProgressDTO::getCompletedCount).sum() - totalExtra;
        long totalRequired = progressList.stream().mapToLong(CourseTypeProgressDTO::getRequiredCount).sum();
        long typesFulfilled = progressList.stream()
                .filter(p -> p.getCompletedCount() >= p.getRequiredCount())
                .count();
        long typesWithRequirement = progressList.size();

        model.addAttribute("progressList", progressList);
        model.addAttribute("totalCompleted", totalCompleted);
        model.addAttribute("totalRequired", totalRequired);
        model.addAttribute("typesFulfilled", typesFulfilled);
        model.addAttribute("typesWithRequirement", typesWithRequirement);

        return "student/graduation-requirements/current-semester";
    }

        @GetMapping("/overall-progress")
        public String overallProgress(HttpSession session, Model model) {
                StudentContext context = buildStudentContext(session, model);

                List<OverallCourseTypeProgressDTO> progressList = srgService
                                .buildOverallProgress(context.stdid(), context.schemeId(), context.splid());

                long totalExtraCourses = progressList.stream().mapToLong(OverallCourseTypeProgressDTO::getExtraCourses).sum();
                long totalCompletedCourses = progressList.stream().mapToLong(OverallCourseTypeProgressDTO::getCompletedCourses).sum() - totalExtraCourses;
                long totalRequiredCourses = progressList.stream().mapToLong(OverallCourseTypeProgressDTO::getMinCourses).sum();
                long totalRemainingCourses = Math.max(0L, totalRequiredCourses - totalCompletedCourses);

                BigDecimal totalExtraCredits = progressList.stream()
                                .map(OverallCourseTypeProgressDTO::getExtraCredits)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalCompletedCredits = progressList.stream()
                                .map(OverallCourseTypeProgressDTO::getCompletedCredits)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .subtract(totalExtraCredits);
                BigDecimal totalRequiredCredits = progressList.stream()
                                .map(OverallCourseTypeProgressDTO::getMinCredits)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalRemainingCredits = totalRequiredCredits.subtract(totalCompletedCredits).max(BigDecimal.ZERO);

                long fullySatisfiedTypes = progressList.stream()
                                .filter(OverallCourseTypeProgressDTO::isFullySatisfied)
                                .count();

                model.addAttribute("progressList", progressList);
                model.addAttribute("totalCompletedCourses", totalCompletedCourses);
                model.addAttribute("totalRequiredCourses", totalRequiredCourses);
                model.addAttribute("totalRemainingCourses", totalRemainingCourses);
                model.addAttribute("totalCompletedCredits", formatDecimal(totalCompletedCredits));
                model.addAttribute("totalRequiredCredits", formatDecimal(totalRequiredCredits));
                model.addAttribute("totalRemainingCredits", formatDecimal(totalRemainingCredits));
                model.addAttribute("typesFullySatisfied", fullySatisfiedTypes);
                model.addAttribute("totalTypes", progressList.size());

                return "student/graduation-requirements/overall-progress";
        }

        @GetMapping("/course-conversions")
        public String courseConversions(HttpSession session, Model model) {
                StudentContext context = buildStudentContext(session, model);

                List<CourseConversionTypeGroupDTO> groups = srgService
                                .buildCourseConversionGroups(context.stdid(), context.schemeId(), context.splid());

                long totalTypes = groups.size();
                long totalCourses = groups.stream().mapToLong(CourseConversionTypeGroupDTO::getTotalCourses).sum();
                long totalMinCourses = groups.stream().mapToLong(CourseConversionTypeGroupDTO::getMinCourses).sum();
                long overMinTypes = groups.stream().filter(CourseConversionTypeGroupDTO::isOverMin).count();
                long changedCourses = groups.stream()
                                .flatMap(group -> group.getCourses().stream())
                                .filter(course -> course.isChangedFromOriginal())
                                .count();

                model.addAttribute("groups", groups);
                model.addAttribute("totalTypes", totalTypes);
                model.addAttribute("totalCourses", totalCourses);
                model.addAttribute("totalMinCourses", totalMinCourses);
                model.addAttribute("overMinTypes", overMinTypes);
                model.addAttribute("changedCourses", changedCourses);

                return "student/graduation-requirements/course-conversions";
        }

        @PostMapping("/course-conversions/change-type")
        @ResponseBody
        public ResponseEntity<Map<String, Object>> changeCourseType(
                        HttpSession session,
                        @RequestParam("srcid") Long srcid,
                        @RequestParam("ctpid") Long ctpid) {

                StudentContext context = buildStudentContext(session, null);
                Map<String, Object> response = new HashMap<>();
                try {
                        srgService.updateCourseTypeConversion(
                                        context.stdid(), context.schemeId(), context.splid(), srcid, ctpid);
                        response.put("success", true);
                        response.put("message", "Course type updated successfully.");
                        return ResponseEntity.ok(response);
                } catch (IllegalArgumentException ex) {
                        response.put("success", false);
                        response.put("message", ex.getMessage());
                        return ResponseEntity.badRequest().body(response);
                }
        }

        private StudentContext buildStudentContext(HttpSession session, Model model) {
                Long stdid = currentLoggedUser(session).getStdId();
                Students student = srgService.getStudent(stdid);

                Long batchId = student.getStdbchid();
                Batches batch = srgService.getBatch(batchId);
                String currentSemester = srgService.getCurrentSemesterName(batchId);
                String studentName = (student.getStdfirstname() != null ? student.getStdfirstname() : "")
                                + (student.getStdlastname() != null ? " " + student.getStdlastname() : "");

                if (model != null) {
                        model.addAttribute("studentName", studentName.trim());
                        model.addAttribute("studentId", student.getStdinstid());
                        model.addAttribute("batchName", batch.getBchname());
                        model.addAttribute("currentSemester", currentSemester);
                }

                return new StudentContext(stdid, batchId, batch.getSchemeId(), batch.getSplid(), currentSemester);
        }

        private LoggedUser currentLoggedUser(HttpSession session) {
                if (session == null) {
                        throw new UnAuthorisedUserException();
                }

                LoggedUser user = (LoggedUser) session.getAttribute(SessionConstants.CURRENT_USER);
                if (user == null) {
                        throw new UnAuthorisedUserException();
                }

                return user;
        }

        private String formatDecimal(BigDecimal value) {
                BigDecimal normalized = (value != null ? value : BigDecimal.ZERO).stripTrailingZeros();
                if (normalized.scale() < 0) {
                        normalized = normalized.setScale(0);
                }
                return normalized.toPlainString();
        }

        private record StudentContext(Long stdid, Long batchId, Long schemeId, Long splid, String currentSemester) {
        }
}

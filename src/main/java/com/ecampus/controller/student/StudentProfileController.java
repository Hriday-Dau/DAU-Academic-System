package com.ecampus.controller.student;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import com.ecampus.model.Batches;
import com.ecampus.model.Programs;
import com.ecampus.model.Students;
import com.ecampus.repository.ProgramsRepository;
import com.ecampus.repository.StudentsRepository;
import com.ecampus.session.SessionConstants;
import com.ecampus.service.StudentGraduationRequirementsService;
import com.ecampus.service.StudentRegistrationService;
import com.ecampus.util.LoggedUser;
import com.ecampus.util.UnAuthorisedUserException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student")
public class StudentProfileController {

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("dd - MMM - yyyy");

    @Autowired
    private StudentsRepository studentsRepo;

    @Autowired
    private StudentRegistrationService registrationService;

    @Autowired
    private StudentGraduationRequirementsService graduationRequirementsService;

    @Autowired
    private ProgramsRepository programsRepo;

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Students student = resolveStudent(session, model);
        if (student == null) {
            return "student/student-profile";
        }

        populateProfileModel(student, model);
        return "student/student-profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(HttpSession session, Model model) {
        Students student = resolveStudent(session, model);
        if (student == null) {
            return "student/student-profile-edit";
        }

        populateProfileModel(student, model);
        return "student/student-profile-edit";
    }

    @PostMapping("/profile/edit")
    @Transactional
    public String updateProfile(HttpSession session,
                                @RequestParam(required = false) String stdheight,
                                @RequestParam(required = false) String stdbldgrp,
                                RedirectAttributes redirectAttributes) {
        Long studentId = resolveStudentId(session);
        if (studentId == null) {
            redirectAttributes.addFlashAttribute("error", "Unable to resolve student account");
            return "redirect:/student/profile";
        }

        Students student = studentsRepo.findStudent(studentId);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Student record not found");
            return "redirect:/student/profile";
        }

        BigDecimal heightValue = parseDecimal(stdheight);
        if (stdheight != null && !stdheight.isBlank() && heightValue == null) {
            redirectAttributes.addFlashAttribute("error", "Height must be a number");
            return "redirect:/student/profile/edit";
        }

        student.setStdheight(heightValue);
        student.setStdbldgrp(normalizeBlank(stdbldgrp));
        studentsRepo.save(student);

        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/student/profile";
    }

    private Students resolveStudent(HttpSession session, Model model) {
        Long studentId = resolveStudentId(session);
        if (studentId == null) {
            model.addAttribute("error", "Unable to resolve student account");
            return null;
        }

        Students student = registrationService.getStudentById(studentId);
        if (student == null) {
            model.addAttribute("error", "Student record not found");
            return null;
        }
        return student;
    }

    private Long resolveStudentId(HttpSession session) {
        LoggedUser user = currentLoggedUser(session);
        Long studentId = user.getStdId();
        String instituteId = user.getUnivId();
        if (instituteId != null && !instituteId.isBlank()) {
            Long latestStudentId = registrationService.getLatestStudentIdByInstituteId(instituteId);
            if (latestStudentId != null) {
                studentId = latestStudentId;
            }
        }

        String username = user.getUserName();
        if (username != null && username.matches("\\d+")) {
            Long mappedByUsername = registrationService.getLatestStudentIdByInstituteId(username);
            if (mappedByUsername != null) {
                studentId = mappedByUsername;
            }
        }

        return studentId;
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

    private void populateProfileModel(Students student, Model model) {
        String studentName = buildStudentName(student);
        String studentPhotoPath = normalizePhotoPath(student.getStdphotolocation());

        Batches batch = null;
        Programs program = null;
        if (student.getStdbchid() != null) {
            batch = graduationRequirementsService.getBatch(student.getStdbchid());
        }
        if (batch != null && batch.getBchprgid() != null) {
            program = programsRepo.getprgId(batch.getBchprgid());
        }

        model.addAttribute("studentName", studentName);
        model.addAttribute("studentInstId", student.getStdinstid());
        model.addAttribute("programmeName", program != null ? program.getPrgname() : "Programme");
        model.addAttribute("batchName", batch != null ? batch.getBchname() : "");
        model.addAttribute("studentPhotoPath", studentPhotoPath);

        model.addAttribute("dobValue", student.getStddob() != null ? DOB_FORMAT.format(student.getStddob()) : "--");
        model.addAttribute("genderValue", safeValue(student.getStdgender()));
        model.addAttribute("qualPercentValue", formatDecimal(student.getStdplustwo()));
        model.addAttribute("qualBoardValue", safeValue(student.getStdplustwoboard()));
        model.addAttribute("qualYearValue", safeValue(student.getStdplustwoyear()));
        model.addAttribute("heightValue", formatDecimal(student.getStdheight()));
        model.addAttribute("idMarkValue", safeValue(student.getStdidmark()));
        model.addAttribute("bloodGroupValue", safeValue(student.getStdbldgrp()));
    }

    private String buildStudentName(Students student) {
        String first = student.getStdfirstname() == null ? "" : student.getStdfirstname();
        String middle = student.getStdmiddlename() == null ? "" : student.getStdmiddlename();
        String last = student.getStdlastname() == null ? "" : student.getStdlastname();
        return String.join(" ", List.of(first, middle, last)).trim().replaceAll("\\s+", " ");
    }

    private String normalizePhotoPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return "/images/students/default-avatar.svg";
        }
        return storedPath.trim();
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "--" : value.trim();
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }
}

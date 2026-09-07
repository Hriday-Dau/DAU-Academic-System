package com.ecampus.controller.student;

import com.ecampus.dto.StudentResultViewDTO;
import com.ecampus.service.StudentResultService;
import com.ecampus.session.SessionConstants;
import com.ecampus.util.LoggedUser;
import com.ecampus.util.UnAuthorisedUserException;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/student")
public class StudentResultController {

    private final StudentResultService studentResultService;

    public StudentResultController(StudentResultService studentResultService) {
        this.studentResultService = studentResultService;
    }

    @GetMapping("/result")
    public String result(@RequestParam(required = false) Long semesterId,
                         HttpSession session,
                         Model model) {
        StudentResultViewDTO result = studentResultService.buildResult(currentStudentId(session), semesterId);
        model.addAttribute("result", result);
        return "student/result";
    }

    private Long currentStudentId(HttpSession session) {
        if (session == null) {
            throw new UnAuthorisedUserException();
        }

        LoggedUser user = (LoggedUser) session.getAttribute(SessionConstants.CURRENT_USER);
        if (user == null || !user.isStudent()) {
            throw new UnAuthorisedUserException();
        }

        return user.getStdId();
    }
}

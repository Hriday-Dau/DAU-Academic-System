package com.ecampus.controller.faculty;

import java.util.List;
import com.ecampus.dto.GradeModDetailDTO;
import com.ecampus.model.GradeModRequests;
import com.ecampus.model.Users;
import com.ecampus.repository.UserRepository;
import com.ecampus.service.GradeModificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/faculty")
public class FacultyGradeModController {

    @Autowired
    private GradeModificationService gradeModService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Grade Modification application form side routes
     */ 

    // to be added..




    /**
     * Route requests for faculty status viewing
     */

    @GetMapping("/pending-requests")
    public String viewPendingRequests(Authentication authentication, Model model) {
        String username = authentication.getName();
        Users user = userRepository.findWithName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long facultyId = user.getUid();

        model.addAttribute("requests", gradeModService.getPendingRequestsForFaculty(facultyId));
        return "faculty/faculty-pending";
    }

    @GetMapping("/completed-requests")
    public String viewCompletedRequests(Authentication authentication, Model model) {
        String username = authentication.getName();
        Users user = userRepository.findWithName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long facultyId = user.getUid();

        model.addAttribute("requests", gradeModService.getCompletedRequestsForFaculty(facultyId));
        return "faculty/faculty-completed";
    }

    @GetMapping("/request-details/{id}")
    public String viewRequestDetails(@PathVariable("id") Long requestId, Model model) {
        // -- Fetching Request Header and Student Details
        GradeModRequests header = gradeModService.getRequestById(requestId);
        List<GradeModDetailDTO> studentDetails = gradeModService.getDetailedStudentList(requestId);

        // dynamic courseinfo string
        String fullCourseInfo = gradeModService.getFormattedCourseInfo(header.getGmdTcrId());

        model.addAttribute("requestId", requestId);
        model.addAttribute("fullCourseInfo", fullCourseInfo);
        model.addAttribute("overallRemarks", header.getGmdReqDesc());
        model.addAttribute("studentDetails", studentDetails);

        return "faculty/faculty-request-details";
    }
}
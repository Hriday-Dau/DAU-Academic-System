package com.ecampus.controller.grademodadmin;

import com.ecampus.dto.GradeModAdminSummaryDTO;
import com.ecampus.model.Users;
import com.ecampus.repository.UserRepository;
import com.ecampus.service.GradeModificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class GradeModAdminController {

    @Autowired
    private GradeModificationService gradeModService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dean/pending-approvals")
    @PreAuthorize("hasAuthority('DEAN')")
    public String viewDeanInbox(Authentication authentication, Model model) {

        Users user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login?error=user_not_found";
        }

        if (!"DEAN".equalsIgnoreCase(user.getrole())) {
            return "redirect:/login?error=unauthorized";
        }

        List<GradeModAdminSummaryDTO> requests = gradeModService.getRequestsForDean();
        model.addAttribute("requests", requests);

        return "grademodadmin/dean-pending-approvals";
    }

    @PostMapping("/dean/process-request")
    @PreAuthorize("hasAuthority('DEAN')")
    public String processRequest(@RequestParam("requestId") Long requestId,
                                 @RequestParam("action") String action,
                                 @RequestParam(value = "remarks", required = false) String remarks,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        Users user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login?error=user_not_found";
        }

        if (!"DEAN".equalsIgnoreCase(user.getrole())) {
            return "redirect:/login?error=unauthorized";
        }

        try {
            gradeModService.processDeanAction(requestId, action, remarks, user.getUid());

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Request #" + requestId + " has been " + action.toLowerCase() + "d."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Failed to process request: " + e.getMessage()
            );
        }

        return "redirect:/dean/pending-approvals";
    }

    @GetMapping("/registrar/pending-approvals")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    public String viewRegistrarInbox(Authentication authentication, Model model) {

        Users user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login?error=user_not_found";
        }

        if (!"REGISTRAR".equalsIgnoreCase(user.getrole())) {
            return "redirect:/login?error=unauthorized";
        }

        List<GradeModAdminSummaryDTO> requests = gradeModService.getRequestsForRegistrar();
        model.addAttribute("requests", requests);

        return "grademodadmin/registrar-pending-approvals";
    }

    @PostMapping("/registrar/process-request")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    public String processRegistrarRequest(@RequestParam("requestId") Long requestId,
                                          @RequestParam("action") String action,
                                          @RequestParam(value = "remarks", required = false) String remarks,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {

        Users user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login?error=user_not_found";
        }

        if (!"REGISTRAR".equalsIgnoreCase(user.getrole())) {
            return "redirect:/login?error=unauthorized";
        }

        try {
            gradeModService.processRegistrarAction(requestId, action, remarks, user.getUid());

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Request #" + requestId + " has been finalized."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Failed to process request: " + e.getMessage()
            );
        }

        return "redirect:/registrar/pending-approvals";
    }

    private Users getAuthenticatedUser(Authentication authentication) {

        if (authentication == null ||
                authentication.getName() == null ||
                authentication.getName().isBlank()) {
            return null;
        }

        String loginValue = authentication.getName();

        Optional<Users> opt = userRepository.findWithName(loginValue);

        return opt.orElse(null);
    }
}
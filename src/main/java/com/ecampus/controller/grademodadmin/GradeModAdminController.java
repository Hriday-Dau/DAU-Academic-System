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

import java.util.Optional;
import java.util.List;

@Controller
public class GradeModAdminController {

    @Autowired
    private GradeModificationService gradeModService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dean/pending-approvals")
    @PreAuthorize("hasAuthority('FACULTY')") // Change this to FACULTY
    public String viewDeanInbox(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Users> opt = userRepository.findWithName(username);

        if (opt.isEmpty()) {
            return "redirect:/login?error=user_not_found";
        }

        Users user = opt.get();
        // Soft Distinction: only Dean AP can proceed (through urole0 and utype0)
        if (!"FACULTY".equals(user.getUrole0()) || !"U".equals(user.getUtype0())) {
            return "redirect:/faculty/dashboard?error=unauthorized";
        }

        List<GradeModAdminSummaryDTO> requests = gradeModService.getRequestsForDean();
        model.addAttribute("requests", requests);
        return "grademodadmin/dean-pending-approvals";
    }

    @PostMapping("/dean/process-request")
    @PreAuthorize("hasAuthority('FACULTY')")
    public String processRequest(@RequestParam("requestId") Long requestId,
            @RequestParam("action") String action,
            @RequestParam(value = "remarks", required = false) String remarks,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // Use repository lookup to obtain the Users entity (avoid casting principal)
        String username = authentication.getName();
        Optional<Users> opt = userRepository.findWithName(username);
        if (opt.isEmpty()) {
            return "redirect:/login?error=user_not_found";
        }
        Users user = opt.get();

        // Security check: Only Dean
        if (!"FACULTY".equals(user.getUrole0()) || !"U".equals(user.getUtype0())) {
            return "redirect:/faculty/dashboard?error=unauthorized";
        }

        try {
            gradeModService.processDeanAction(requestId, action, remarks, user.getUid());
            redirectAttributes.addFlashAttribute("success",
                    "Request #" + requestId + " has been " + action.toLowerCase() + "d.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process request: " + e.getMessage());
        }

        return "redirect:/dean/pending-approvals";
    }

    @GetMapping("/registrar/pending-approvals")
    @PreAuthorize("hasAuthority('FACULTY')")
    public String viewRegistrarInbox(Authentication authentication, Model model) {
        //Using Repository lookup instead of direct casting
        String username = authentication.getName();
        Optional<Users> opt = userRepository.findWithName(username);

        if (opt.isEmpty()) {
            return "redirect:/login?error=user_not_found";
        }

        Users user = opt.get();
        // Security Check: Only Registrar allowed
        if (!"EMPLOYEE".equals(user.getUrole0()) || !"U".equals(user.getUtype0())) {
            return "redirect:/faculty/dashboard?error=unauthorized";
        }

        List<GradeModAdminSummaryDTO> requests = gradeModService.getRequestsForRegistrar();
        model.addAttribute("requests", requests);
        return "grademodadmin/registrar-pending-approvals";
    }

    @PostMapping("/registrar/process-request")
    @PreAuthorize("hasAuthority('FACULTY')")
    public String processRegistrarRequest(@RequestParam("requestId") Long requestId,
            @RequestParam("action") String action,
            @RequestParam(value = "remarks", required = false) String remarks,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // again, usiing Repository lookup instead of direct casting
        String username = authentication.getName();
        Optional<Users> opt = userRepository.findWithName(username);

        if (opt.isEmpty())
            return "redirect:/login";
        Users user = opt.get();

        if (!"EMPLOYEE".equals(user.getUrole0()) || !"U".equals(user.getUtype0())) {
            return "redirect:/faculty/dashboard?error=unauthorized";
        }

        gradeModService.processRegistrarAction(requestId, action, remarks, user.getUid());
        redirectAttributes.addFlashAttribute("success", "Request #" + requestId + " has been finalized.");
        return "redirect:/registrar/pending-approvals";
    }
}
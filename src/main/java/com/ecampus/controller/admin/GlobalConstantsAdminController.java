package com.ecampus.controller.admin;

import com.ecampus.service.GlobalConstantsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/global-constants")
public class GlobalConstantsAdminController {

    private final GlobalConstantsService globalConstantsService;

    public GlobalConstantsAdminController(GlobalConstantsService globalConstantsService) {
        this.globalConstantsService = globalConstantsService;
    }

    @GetMapping
    public String showGlobalConstants(Model model) {
        model.addAttribute("currentAcademicYearId", globalConstantsService.getCurrentAcademicYearId());
        model.addAttribute("currentAcademicYearName", globalConstantsService.getCurrentAcademicYearName());
        model.addAttribute("currentTermId", globalConstantsService.getCurrentTermId());
        model.addAttribute("currentTermName", globalConstantsService.getCurrentTermName());
        return "admin/global-constants";
    }

    @PostMapping("/academic-context")
    public String updateAcademicContext(@RequestParam String academicYear,
                                        @RequestParam String termName,
                                        RedirectAttributes redirectAttributes) {
        try {
            globalConstantsService.updateAcademicContext(academicYear, termName);
            redirectAttributes.addFlashAttribute("success", "Current academic context updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/global-constants";
    }
}


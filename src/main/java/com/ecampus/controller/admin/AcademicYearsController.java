package com.ecampus.controller.admin;

import com.ecampus.model.AcademicYears;
import com.ecampus.repository.AcademicYearsRepository;
import com.ecampus.service.GlobalConstantsService;
import com.ecampus.session.SessionConstants;
import com.ecampus.util.LoggedUser;

import jakarta.servlet.http.HttpSession;

// import com.ecampus.service.GlobalConstantsService;
// import com.ecampus.util.LoggedUser;
// import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/academicyears")
public class AcademicYearsController {

    @Autowired
    private AcademicYearsRepository academicYearsRepository;

    private final GlobalConstantsService globalConstantsService;

    // Inject the necessary repositories and services
    public AcademicYearsController(AcademicYearsRepository academicYearsRepository,
            GlobalConstantsService globalConstantsService) {
        this.academicYearsRepository = academicYearsRepository;
        this.globalConstantsService = globalConstantsService;
    }

    // 1. LIST ALL ACADEMIC YEARS
    @GetMapping
    public String listAcademicYears(Model model) {
        model.addAttribute("academicyears",
                academicYearsRepository.findAll()
                        .stream()
                        .sorted((a, b) -> b.getAyrid().compareTo(a.getAyrid())) // DESC
                        .toList());
        return "admin/academic-years";
    }

    // 2. SHOW ADD FORM
    @GetMapping("/add")
    public String showAddForm(Model model) {
        // Fetch current academic year name (e.g., "2025-26")
        String currentAyrName = globalConstantsService.getCurrentAcademicYearName();
        int nextStartYear = 2026; // Safe fallback

        // Parse the first 4 characters to get the start year, then add 1
        if (currentAyrName != null && currentAyrName.length() >= 4) {
            try {
                int currentStartYear = Integer.parseInt(currentAyrName.substring(0, 4));
                nextStartYear = currentStartYear + 1;
            } catch (NumberFormatException e) {
                // Fallback to default if parsing fails
            }
        }

        model.addAttribute("nextStartYear", nextStartYear);

        return "admin/academic-year-form";
    }

    // 3. HANDLE ADD FORM SUBMISSION
    @PostMapping("/add")
    public String saveAcademicYear(@RequestParam("startYear") int startYear,
            RedirectAttributes redirectAttributes, HttpSession session) {
        
        // --- NEW SESSION LOGIC ---
        // LoggedUser currentUser = (LoggedUser) session.getAttribute(SessionConstants.CURRENT_USER);
        // if (currentUser == null) {
        //     return "redirect:/login";
        // }
        // Long currentUserId = currentUser.getUid();
        // -------------------------        
                
        // 1. Generate ayrname from startYear
        String ayrname = startYear + "-" + String.valueOf(startYear + 1).substring(2);

        // 2. Check if already exists
        Long existingId = academicYearsRepository.findAcademicYearIdByName(ayrname);
        if (existingId != null) {
            redirectAttributes.addFlashAttribute("error", "Academic year " + ayrname + " already exists.");
            return "redirect:/admin/academicyears/add";
        }

        // 3. Fetch current max ID
        Long maxId = academicYearsRepository.findMaxAyrid();
        Long newId = (maxId == null ? 0 : maxId) + 1;

        // 4. Create and save
        AcademicYears ay = new AcademicYears();
        ay.setAyrid(newId);
        ay.setAyrname(ayrname);
        ay.setAyrcreatedat(LocalDateTime.now());
        ay.setAyrlastupdatedat(LocalDateTime.now());

        // 
        ay.setAyrcreatedby(1L);
        ay.setAyrlastupdatedby(1L);
        ay.setAyrrowstate(1L);

        academicYearsRepository.save(ay);

        // ADD THIS LINE
        redirectAttributes.addFlashAttribute("success", "Academic year " + ayrname + " added successfully!");

        return "redirect:/admin/academicyears";
    }
}
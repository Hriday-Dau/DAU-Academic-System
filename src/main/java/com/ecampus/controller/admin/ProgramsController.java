package com.ecampus.controller.admin;

import com.ecampus.model.Programs;
import com.ecampus.model.Scheme;
import com.ecampus.repository.ProgramsRepository;
import com.ecampus.repository.SchemeRepository;
// new session import
import com.ecampus.session.SessionConstants;
import com.ecampus.util.LoggedUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/programs")
public class ProgramsController {

    private final ProgramsRepository programsRepository;
    private final SchemeRepository schemeRepository;
    public ProgramsController(ProgramsRepository programsRepository, 
                              SchemeRepository schemeRepository) {
        this.programsRepository = programsRepository;
        this.schemeRepository = schemeRepository;
    }

    // UPDATE THIS: Only show Active programs on the master page
    @GetMapping
    public String showPrograms(Model model) {
        model.addAttribute("programs", programsRepository.findActivePrograms());
        return "admin/programs";
    }

    // NEW: Show the Archived Programs Page
    @GetMapping("/archived")
    public String showArchivedPrograms(Model model) {
        model.addAttribute("programs", programsRepository.findArchivedPrograms());
        return "admin/archived-programs";
    }

    // NEW: Archive a program
    @GetMapping("/{id}/archive")
    public String archiveProgram(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        // --- NEW SESSION LOGIC ---
        LoggedUser currentUser = (LoggedUser) session.getAttribute(SessionConstants.CURRENT_USER);
        if (currentUser == null) {
            return "redirect:/login";
        }
        // -------------------------

        Programs program = programsRepository.findById(id).orElseThrow();
        
        // NEW: Increment version by 1, then shift into archived range (+900)
        // eg., if current state is 3, it becomes 4 + 900 = 904.
        Long currentVersion = program.getPrgrowstate();
        program.setPrgrowstate((currentVersion + 1L) + 900L);

        program.setPrglastupdatedby(currentUser.getUID());
        program.setPrglastupdatedat(LocalDateTime.now());
        
        programsRepository.save(program);
        redirectAttributes.addFlashAttribute("success", "Program archived successfully.");
        return "redirect:/admin/programs";
    }

    // NEW: Restore a program
    @GetMapping("/{id}/restore")
    public String restoreProgram(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        // --- NEW SESSION LOGIC ---
        LoggedUser currentUser = (LoggedUser) session.getAttribute(SessionConstants.CURRENT_USER);
        if (currentUser == null) {
            return "redirect:/login";
        }
        // -------------------------

        Programs program = programsRepository.findById(id).orElseThrow();
        
        // NEW: Bring back to active range (-900), then increment version by 1
        // e.g., if archived state is 904, it becomes 4 + 1 = 5.
        Long currentArchivedState = program.getPrgrowstate();
        program.setPrgrowstate((currentArchivedState - 900L) + 1L);

        program.setPrglastupdatedby(currentUser.getUID());
        program.setPrglastupdatedat(LocalDateTime.now());
        
        programsRepository.save(program);
        redirectAttributes.addFlashAttribute("success", "Program restored successfully.");
        // 
        return "redirect:/admin/programs/archived";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("program", new Programs());
        model.addAttribute("editing", false);
        return "admin/program-form";
    }

    @PostMapping("/save")
    public String saveProgram(@ModelAttribute("program") Programs program, RedirectAttributes redirectAttributes, HttpSession session) {
        
        // --- NEW SESSION LOGIC ---
        LoggedUser currentUser = (LoggedUser) session.getAttribute(SessionConstants.CURRENT_USER);
        if (currentUser == null) {
            return "redirect:/login";
        }
        Long currentUserId = currentUser.getUID();
        // -------------------------

        LocalDateTime now = LocalDateTime.now();
        
        boolean isEditing = (program.getPrgid() != null);

        // --- 1. ENFORCE DURATION UNIT ---
        program.setPrgdurationunits("Years");

        if (!isEditing) {
            // --- LOGIC FOR NEW PROGRAM ---

            // --- 2. VALIDATION: Check for duplicate Institute Code ---
            if (programsRepository.existsByInstituteCode(program.getPrginstcode())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Institute Code '" + program.getPrginstcode() + "' is already taken. Please choose a different one.");
                // Redirect back to the form (the input data will be lost, but they are warned)
                return "redirect:/admin/programs/add"; 
            }
            
            // 1. Auto-assign ID
            Long maxId = programsRepository.findMaxPrgid();
            program.setPrgid(maxId == null ? 1L : maxId + 1L);
            
            // 2. Set Creation Audit Fields
            program.setPrgcreatedby(currentUserId);
            program.setPrgcreatedat(now);
            program.setPrgrowstate(1L); // 1 = Active
            
        } else {
            // --- LOGIC FOR EDITING PROGRAM ---
            // 1. Fetch the existing record to retain its original creation data
            Programs existingProgram = programsRepository.findById(program.getPrgid()).orElseThrow();
            
            // Re-assert uneditable fields directly from the database 
            // (Provides security in case a user inspects element and removes the 'readonly' tag in the browser)
            program.setPrginstcode(existingProgram.getPrginstcode());
            program.setPrglevel(existingProgram.getPrglevel());
            program.setPrgduration(existingProgram.getPrgduration());
            
            program.setPrgcreatedby(existingProgram.getPrgcreatedby());
            program.setPrgcreatedat(existingProgram.getPrgcreatedat());
            program.setPrgrowstate(existingProgram.getPrgrowstate() + 1L);
            
            program.setPrglastupdatedby(currentUserId);
            program.setPrglastupdatedat(now);
        }

        // Save to Database
        Programs savedProgram = programsRepository.save(program);

        // Redirect appropriately
        if (isEditing) {
            // If we just added input validation later, you'd add success flashes here

            redirectAttributes.addFlashAttribute("success", "Program updated successfully.");
            return "redirect:/admin/programs/" + savedProgram.getPrgid() + "/edit"; // Redirect to the edit page for the updated program
        } else {
            redirectAttributes.addFlashAttribute("success", "Program added successfully.");
            return "redirect:/admin/programs";
        }
    }

    // 1. VIEW MODE (Read-Only)
    @GetMapping("/{id}/view")
    public String viewProgramSchemes(@PathVariable("id") Long programId, Model model) {
        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        List<Scheme> schemes = schemeRepository.findByProgram_Prgid(programId);

        model.addAttribute("program", program.get());
        model.addAttribute("schemes", schemes);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", false);
        
        return "admin/program-schemes";
    }

    // 2. EDIT MODE (Manage Program & Schemes)
    @GetMapping("/{id}/edit")
    public String manageProgramSchemes(@PathVariable("id") Long programId, Model model) {
        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        List<Scheme> schemes = schemeRepository.findByProgram_Prgid(programId);
        
        model.addAttribute("program", program.get());
        model.addAttribute("schemes", schemes);
        
        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true); 
        
        return "admin/program-schemes";
    }

    @GetMapping("/{id}/edit/info")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Programs program = programsRepository.findById(id).orElseThrow();
        model.addAttribute("program", program);
        model.addAttribute("editing", true);
        
        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);

        return "admin/program-form";
    }
}
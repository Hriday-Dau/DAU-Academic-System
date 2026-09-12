package com.ecampus.controller.admin;

import com.ecampus.model.CourseTypes;
import com.ecampus.model.Programs;
import com.ecampus.model.Scheme;
import com.ecampus.repository.CourseTypesRepository;
import com.ecampus.repository.ProgramsRepository;
import com.ecampus.repository.SchemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
// @RequestMapping("/admin/programs/{programId}/schemes/{schemeId}/{splid}/course-types")
@RequestMapping("/admin/programs/{programId}")
public class CourseTypesController {

    @Autowired
    private CourseTypesRepository courseTypesRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ProgramsRepository programsRepository;

    // Course Types view-only page
    @GetMapping("/view/schemes/{schemeId}/{splid}/course-types")
    public String viewCourseTypes(@PathVariable Long programId,
                                  @PathVariable Long schemeId,
                                  @PathVariable Long splid,
                                  Model model) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid scheme ID"));

        List<CourseTypes> courseTypes =
                courseTypesRepository.findBySchemeIdAndSplidOrderByCtpid(schemeId, splid);

        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        String programName = program.get().getPrgname();

        model.addAttribute("scheme", scheme);
        model.addAttribute("programId", programId);
        model.addAttribute("splid", splid);
        model.addAttribute("courseTypes", courseTypes);
        model.addAttribute("programName", programName);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", false);

        return "admin/course-types";
    }

    //Course Types editable page
    @GetMapping("/edit/schemes/{schemeId}/{splid}/course-types")
    public String editCourseTypes(@PathVariable Long programId,
                                  @PathVariable Long schemeId,
                                  @PathVariable Long splid,
                                  Model model) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid scheme ID"));

        List<CourseTypes> courseTypes =
                courseTypesRepository.findBySchemeIdAndSplidOrderByCtpid(schemeId, splid);

        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        String programName = program.get().getPrgname();

        model.addAttribute("scheme", scheme);
        model.addAttribute("programId", programId);
        model.addAttribute("splid", splid);
        model.addAttribute("courseTypes", courseTypes);
        model.addAttribute("programName", programName);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);

        return "admin/course-types";
    }

    @GetMapping("/edit/schemes/{schemeId}/{splid}/course-types/add")
    public String showAddCourseTypeForm(@PathVariable Long programId,
                                        @PathVariable Long schemeId,
                                        @PathVariable Long splid,
                                        Model model) {
        CourseTypes ct = new CourseTypes();
        ct.setSchemeId(schemeId);
        ct.setSplid(splid);

        model.addAttribute("courseType", ct);
        model.addAttribute("editing", false);
        model.addAttribute("programId", programId);
        model.addAttribute("scheme", schemeRepository.findById(schemeId).orElse(null));
        model.addAttribute("splid", splid);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);

        return "admin/course-type-form";
    }

    @GetMapping("/edit/schemes/{schemeId}/{splid}/course-types/{ctpid}/edit")
    public String showEditCourseTypeForm(@PathVariable Long programId,
                                         @PathVariable Long schemeId,
                                         @PathVariable Long splid,
                                         @PathVariable Long ctpid,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        Optional<CourseTypes> ctOpt = courseTypesRepository.findById(ctpid);
        if (ctOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Course type not found.");
            return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId + "/" + splid + "/course-types";
        }
        CourseTypes ct = ctOpt.get();

        model.addAttribute("courseType", ct);
        model.addAttribute("editing", true);
        model.addAttribute("programId", programId);
        model.addAttribute("scheme", schemeRepository.findById(schemeId).orElse(null));
        model.addAttribute("splid", splid);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);

        return "admin/course-type-form";
    }

    @PostMapping("/edit/schemes/{schemeId}/{splid}/course-types/save")
    public String saveCourseType(@PathVariable Long programId,
                                 @PathVariable Long schemeId,
                                 @PathVariable Long splid,
                                 @RequestParam("editing") boolean editing,
                                 @ModelAttribute("courseType") CourseTypes courseType,
                                 RedirectAttributes redirectAttributes) {
        // ensure key fields
        courseType.setSchemeId(schemeId);
        courseType.setSplid(splid);

        if (!editing) {
            // Generate new ctpid for new entries
            Long maxCtpid = courseTypesRepository.findMaxCtpid();
            courseType.setCtpid(maxCtpid + 1);
        }

        if (!editing && courseType.getCtpid() != null && courseTypesRepository.existsById(courseType.getCtpid())) {
            redirectAttributes.addFlashAttribute("error", "Course Type with this ID already exists.");
            return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId + "/" + splid + "/course-types/add";
        }

        courseTypesRepository.save(courseType);
        return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId + "/" + splid + "/course-types";
    }

    @GetMapping("/edit/schemes/{schemeId}/{splid}/course-types/{ctpid}/delete")
    public String deleteCourseType(@PathVariable Long programId,
                                   @PathVariable Long schemeId,
                                   @PathVariable Long splid,
                                   @PathVariable Long ctpid,
                                   RedirectAttributes redirectAttributes) {
        if (!courseTypesRepository.existsById(ctpid)) {
            redirectAttributes.addFlashAttribute("error", "Course type not found.");
        } else {
            try {
                courseTypesRepository.deleteById(ctpid);
            } catch (DataIntegrityViolationException ex) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete this course type because it is already in use.");
            }
        }
        return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId + "/" + splid + "/course-types";
    }
}
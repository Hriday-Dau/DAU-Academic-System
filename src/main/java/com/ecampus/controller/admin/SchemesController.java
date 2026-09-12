package com.ecampus.controller.admin;

import com.ecampus.model.*;
import com.ecampus.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.List;
@Controller
// @RequestMapping("/admin/programs/{programId}/schemes")
@RequestMapping("/admin/programs/{programId}")
public class SchemesController {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ProgramsRepository programsRepository;
    @Autowired
    private CourseTypesRepository courseTypesRepository;
    @Autowired
    private SchemeCoursesRepository schemeCourseRepository;
    @Autowired
    private SchemeDetailsRepository schemeDetailsRepository;
    // Show Add Scheme form
    @GetMapping("/edit/schemes/add")
    public String showAddSchemeForm(@PathVariable("programId") Short programId, Model model) {
        Optional<Programs> program = programsRepository.findById(Long.valueOf(programId));
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }

        Scheme scheme = new Scheme();
        scheme.setProgram(program.get());
        model.addAttribute("scheme", scheme);
        model.addAttribute("editing", false);
        return "admin/scheme-form";
    }

    // Handle Scheme submission
    @PostMapping("/edit/schemes/save")
    public String saveScheme(@ModelAttribute("scheme") Scheme scheme,
                             @RequestParam("editing") boolean editing,
                             RedirectAttributes redirectAttributes) {

        if (!editing && schemeRepository.existsById(scheme.getId())) {
            redirectAttributes.addFlashAttribute("error", "Scheme with this ID already exists.");
            return "redirect:/admin/programs/" + scheme.getProgram().getPrgid() + "/edit/schemes/add";
        }

        schemeRepository.save(scheme);
        return "redirect:/admin/programs/" + scheme.getProgram().getPrgid() + "/edit";
    }

    //View Scheme Page
    @GetMapping("/view/schemes/{schemeId}")
    public String viewSchemeDetails(@PathVariable Long programId,
                                    @PathVariable Long schemeId,
                                    Model model) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid scheme ID"));

        List<SchemeDetails> degrees =
                schemeDetailsRepository.findBySchemeId(schemeId);

        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        String programName = program.get().getPrgname();

        model.addAttribute("scheme", scheme);
        model.addAttribute("programId", programId);
        model.addAttribute("degrees", degrees);
        model.addAttribute("programName", programName);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", false);

        return "admin/schemes";
    }

    //Edit Scheme Page
    @GetMapping("/edit/schemes/{schemeId}")
    public String editSchemeDetails(@PathVariable Long programId,
                                    @PathVariable Long schemeId,
                                    Model model) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid scheme ID"));

        List<SchemeDetails> degrees =
                schemeDetailsRepository.findBySchemeId(schemeId);

        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        String programName = program.get().getPrgname();

        model.addAttribute("scheme", scheme);
        model.addAttribute("programId", programId);
        model.addAttribute("degrees", degrees);
        model.addAttribute("programName", programName);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);

        return "admin/schemes";
    }

    // View-only Graduation requirements page
    @GetMapping("/view/schemes/{schemeId}/{splid}/graduation-requirements")
    public String viewGraduationRequirements(@PathVariable Long programId,
                                             @PathVariable Long schemeId,
                                             @PathVariable Long splid,
                                             Model model) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid scheme ID"));

        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        String programName = program.get().getPrgname();

        Optional<SchemeDetails> degreeOpt = schemeDetailsRepository.findBySchemeIdAndSplid(schemeId, splid);
        if (degreeOpt.isEmpty()) {
            return "redirect:/admin/programs/" + programId + "/view/schemes/" + schemeId;
        }
        SchemeDetails degree = degreeOpt.get();
 
        model.addAttribute("scheme", scheme);
        model.addAttribute("programId", programId);
        model.addAttribute("degree", degree);
        model.addAttribute("programName", programName);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", false);


        return "admin/graduation-requirements";
    }

    // Open Graduation requirements in Edit Mode
    @GetMapping("/edit/schemes/{schemeId}/{splid}/graduation-requirements")
    public String editGraduationRequirements(@PathVariable Long programId,
                                             @PathVariable Long schemeId,
                                             @PathVariable Long splid,
                                             Model model) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid scheme ID"));

        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }
        String programName = program.get().getPrgname();

        Optional<SchemeDetails> degreeOpt = schemeDetailsRepository.findBySchemeIdAndSplid(schemeId, splid);
        if (degreeOpt.isEmpty()) {
            return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId;
        }
        SchemeDetails degree = degreeOpt.get();

        model.addAttribute("scheme", scheme);
        model.addAttribute("programId", programId);
        model.addAttribute("degree", degree);
        model.addAttribute("programName", programName);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);


        return "admin/graduation-requirements";
    }

    @GetMapping("/edit/schemes/{schemeId}/{splid}/graduation-requirements/edit")
    public String showEditGraduationForm(@PathVariable Long programId,
                                         @PathVariable Long schemeId,
                                         @PathVariable Long splid,
                                         Model model) {
        Optional<SchemeDetails> degreeOpt = schemeDetailsRepository.findBySchemeIdAndSplid(schemeId, splid);
        if (degreeOpt.isEmpty()) {
            return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId;
        }
        SchemeDetails degree = degreeOpt.get();

        model.addAttribute("degree", degree);
        model.addAttribute("programId", programId);
        model.addAttribute("scheme", schemeRepository.findById(schemeId).orElse(null));

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);

        return "admin/graduation-requirements-form";
    }

    @PostMapping("/edit/schemes/{schemeId}/{splid}/graduation-requirements/save")
    public String saveGraduationRequirements(@PathVariable Long programId,
                                             @PathVariable Long schemeId,
                                             @PathVariable Long splid,
                                             @ModelAttribute("degree") SchemeDetails degree) {
        // Ensure composite key fields are properly set
        degree.setSchemeId(schemeId);
        degree.setSplid(splid);
        schemeDetailsRepository.save(degree);
        return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId + "/" + splid + "/graduation-requirements";
    }

    @GetMapping("/edit/schemes/{schemeId}/add")
    public String showAddSpecializationForm(@PathVariable Long programId,
                                            @PathVariable Long schemeId,
                                            Model model) {
        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }

        SchemeDetails sd = new SchemeDetails();
        sd.setSchemeId(schemeId);
        // sd.setSplid(0L);

        model.addAttribute("degree", sd);
        model.addAttribute("programId", programId);
        model.addAttribute("scheme", schemeRepository.findById(schemeId).orElse(null));
        model.addAttribute("editing", false);

        // Pass the mode state to the frontend
        model.addAttribute("isEditable", true);

        return "admin/degree-form";
    }

    @PostMapping("/edit/schemes/{schemeId}/degree/save")
    public String saveSpecialization(@PathVariable Long programId,
                                     @PathVariable Long schemeId,
                                     @ModelAttribute("degree") SchemeDetails degree) {
        degree.setSchemeId(schemeId);
        schemeDetailsRepository.save(degree);
        return "redirect:/admin/programs/" + programId + "/edit/schemes/" + schemeId;
    }

    @GetMapping("/edit/schemes/pull")
    public String showPullSchemeForm(@PathVariable Long programId, Model model) {
        Optional<Programs> program = programsRepository.findById(programId);
        if (program.isEmpty()) {
            return "redirect:/admin/programs";
        }

        // Get all existing schemes for this program
        List<Scheme> existingSchemes = schemeRepository.findByProgram_Prgid(programId);

        model.addAttribute("programId", programId);
        model.addAttribute("existingSchemes", existingSchemes);
        return "admin/pull-scheme-form";
    }

    @PostMapping("/edit/schemes/pull")
    public String pullFromScheme(@PathVariable Long programId,
                                 @RequestParam Long newSchemeId,
                                 @RequestParam Long effectiveFromYear,
                                 @RequestParam Long sourceSchemeId,
                                 RedirectAttributes redirectAttributes) {

        // Check if new scheme ID already exists
        if (schemeRepository.existsById(newSchemeId)) {
            redirectAttributes.addFlashAttribute("error", "Scheme with ID " + newSchemeId + " already exists.");
            return "redirect:/admin/programs/" + programId + "/edit/schemes/pull";
        }

        // Fetch the source scheme
        Optional<Scheme> sourceSchemeOpt = schemeRepository.findById(sourceSchemeId);
        if (sourceSchemeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Source scheme not found.");
            return "redirect:/admin/programs/" + programId + "/edit/schemes/pull";
        }
        // Scheme sourceScheme = sourceSchemeOpt.get();

        // Fetch the program
        Optional<Programs> programOpt = programsRepository.findById(programId);
        if (programOpt.isEmpty()) {
            return "redirect:/admin/programs";
        }

        // 1. Create new Scheme
        Scheme newScheme = new Scheme();
        newScheme.setId(newSchemeId);
        newScheme.setProgram(programOpt.get());
        newScheme.setEffectiveFromYear(effectiveFromYear);
        schemeRepository.save(newScheme);

        // 2. Copy all SchemeDetails (specializations)
        List<SchemeDetails> sourceDetails = schemeDetailsRepository.findBySchemeId(sourceSchemeId);
        for (SchemeDetails sd : sourceDetails) {
            SchemeDetails newDetail = new SchemeDetails();
            newDetail.setSchemeId(newSchemeId);
            newDetail.setSplid(sd.getSplid());
            newDetail.setSplname(sd.getSplname());
            newDetail.setSpldesc(sd.getSpldesc());
            newDetail.setSplMinCreditLoad(sd.getSplMinCreditLoad());
            newDetail.setSplMaxCreditLoad(sd.getSplMaxCreditLoad());
            newDetail.setSplMinCourses(sd.getSplMinCourses());
            newDetail.setSplMaxCourses(sd.getSplMaxCourses());
            newDetail.setSplMinCpi(sd.getSplMinCpi());
            newDetail.setSplMinCredits(sd.getSplMinCredits());
            schemeDetailsRepository.save(newDetail);
        }

        // 3. Copy all CourseTypes for all specializations
        List<CourseTypes> sourceCourseTypes = courseTypesRepository.findBySchemeId(sourceSchemeId);
        Long maxCtpid = courseTypesRepository.findMaxCtpid();
        java.util.Map<Long, Long> ctpidMapping = new java.util.HashMap<>(); // old ctpid -> new ctpid
        for (CourseTypes ct : sourceCourseTypes) {
            maxCtpid++;
            CourseTypes newCt = new CourseTypes();
            newCt.setCtpid(maxCtpid);
            newCt.setSchemeId(newSchemeId);
            newCt.setSplid(ct.getSplid());
            newCt.setCtpcode(ct.getCtpcode());
            newCt.setCrscat(ct.getCrscat());
            newCt.setCtpname(ct.getCtpname());
            newCt.setCtpdesc(ct.getCtpdesc());
            newCt.setMinCourses(ct.getMinCourses());
            newCt.setMaxCourses(ct.getMaxCourses());
            newCt.setMinCredits(ct.getMinCredits());
            newCt.setMaxCredits(ct.getMaxCredits());
            courseTypesRepository.save(newCt);
            ctpidMapping.put(ct.getCtpid(), maxCtpid);
        }

        // 4. Copy all SchemeCourses for all specializations
        List<SchemeCourses> sourceSchemeCourses = schemeCourseRepository.findBySchemeId(sourceSchemeId);
        for (SchemeCourses sc : sourceSchemeCourses) {
            SchemeCourses newSc = new SchemeCourses();
            newSc.setSchemeId(newSchemeId);
            newSc.setSplid(sc.getSplid());
            newSc.setTermName(sc.getTermName());
            newSc.setProgramYear(sc.getProgramYear());
            newSc.setCourseSrNo(sc.getCourseSrNo());
            newSc.setSemNo(sc.getSemNo());
            newSc.setSemesterName(sc.getSemesterName());
            newSc.setTermSeqNo(sc.getTermSeqNo());
            newSc.setCtpid(ctpidMapping.getOrDefault(sc.getCtpid(), sc.getCtpid()));
            newSc.setCrsid(sc.getCrsid());
            newSc.setCourseTitle(sc.getCourseTitle());
            schemeCourseRepository.save(newSc);
        }

        redirectAttributes.addFlashAttribute("success", "Scheme Pulled Successfully!");
        return "redirect:/admin/programs/" + programId + "/edit";
    }

}

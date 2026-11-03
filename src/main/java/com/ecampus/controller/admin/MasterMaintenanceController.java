package com.ecampus.controller.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecampus.dto.master.BatchCreateForm;
import com.ecampus.dto.master.BatchMasterViewDTO;
import com.ecampus.dto.master.RoleCreateForm;
import com.ecampus.dto.master.StudentImportResult;
import com.ecampus.dto.master.UserCreateForm;
import com.ecampus.model.Batches;
import com.ecampus.model.Programs;
import com.ecampus.model.Role;
import com.ecampus.model.Scheme;
import com.ecampus.model.SchemeDetails;
import com.ecampus.model.Students;
import com.ecampus.model.Users;
import com.ecampus.repository.ProgramsRepository;
import com.ecampus.repository.SchemeDetailsRepository;
import com.ecampus.repository.SchemeRepository;
import com.ecampus.repository.UserRepository;
import com.ecampus.service.MasterMaintenanceService;

@Controller
@RequestMapping("/admin/master-maintenance")
public class MasterMaintenanceController {

    @Autowired
    private MasterMaintenanceService masterMaintenanceService;

    @Autowired
    private ProgramsRepository programsRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeDetailsRepository schemeDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    private Long getAdminId(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            Long uid = userRepository.findUidByUname(authentication.getName());
            if (uid != null) {
                return uid;
            }
        }
        return 0L;
    }

    // =========================================================================
    // Master Maintenance Hub
    // =========================================================================

    @GetMapping
    public String showHub() {
        return "admin/mm_hub";
    }

    
    // Module 1: Faculty User Management
    

    @GetMapping("/faculty")
    public String listFaculty(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Users> facultyList = masterMaintenanceService.getFacultyList(search);
        model.addAttribute("facultyList", facultyList);
        model.addAttribute("search", search);
        return "admin/mm_faculty_list";
    }

    @GetMapping("/faculty/add")
    public String showAddFacultyForm(Model model) {
        UserCreateForm form = new UserCreateForm();
        form.setUid(masterMaintenanceService.getNextUid());
        form.setUrole("913");
        model.addAttribute("form", form);
        return "admin/mm_faculty_add";
    }

    @PostMapping("/faculty/add")
    public String saveFacultyUser(@ModelAttribute("form") UserCreateForm form,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            Long adminId = getAdminId(authentication);
            masterMaintenanceService.createFacultyUser(form, adminId);
            redirectAttributes.addFlashAttribute("success", "Faculty user '" + form.getUfullname() + "' added successfully! Initial password set to 'Daiict@26'.");
            return "redirect:/admin/master-maintenance/faculty";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/admin/master-maintenance/faculty/add";
        }
    }

    
    // Module 2: Batches & Student Batch Import
    

    @GetMapping("/batches")
    public String listBatches(@RequestParam(value = "search", required = false) String search, Model model) {
        List<BatchMasterViewDTO> batches = masterMaintenanceService.getBatchesList(search);
        model.addAttribute("batches", batches);
        model.addAttribute("search", search);
        return "admin/mm_batches_list";
    }

    @GetMapping("/batches/add")
    public String showAddBatchForm(Model model) {
        BatchCreateForm form = new BatchCreateForm();
        form.setBchid(masterMaintenanceService.getNextBatchId());
        form.setBchcapacity(400L);
        form.setBchinstcode("DAU");

        List<Programs> programs = programsRepository.findActivePrograms();
        model.addAttribute("form", form);
        model.addAttribute("programs", programs);
        return "admin/mm_batch_add";
    }

    @PostMapping("/batches/add")
    public String saveBatch(@ModelAttribute("form") BatchCreateForm form,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Long adminId = getAdminId(authentication);
            Batches batch = masterMaintenanceService.createBatch(form, adminId);
            redirectAttributes.addFlashAttribute("success", "Batch '" + batch.getBchname() + "' created successfully (Batch ID: " + batch.getBchid() + ").");
            return "redirect:/admin/master-maintenance/batches";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/admin/master-maintenance/batches/add";
        }
    }

    @GetMapping("/batches/{id}")
    public String viewBatchDetails(@PathVariable("id") Long id, Model model) {
        Batches batch = masterMaintenanceService.getBatchById(id);
        List<Students> students = masterMaintenanceService.getStudentsInBatch(id);
        model.addAttribute("batch", batch);
        model.addAttribute("students", students);
        return "admin/mm_batch_detail";
    }

    @GetMapping("/batches/{id}/edit")
    public String showEditBatchForm(@PathVariable("id") Long id, Model model) {
        Batches batch = masterMaintenanceService.getBatchById(id);
        BatchCreateForm form = new BatchCreateForm();
        form.setBchid(batch.getBchid());
        form.setBchprgid(batch.getBchprgid());
        form.setBchname(batch.getBchname());
        form.setBchcapacity(batch.getBchcapacity());
        form.setBchinstcode(batch.getBchinstcode());
        form.setSchemeId(batch.getSchemeId());
        form.setSplid(batch.getSplid());

        model.addAttribute("batch", batch);
        model.addAttribute("form", form);
        return "admin/mm_batch_edit";
    }

    @PostMapping("/batches/{id}/edit")
    public String updateBatch(@PathVariable("id") Long id,
                              @ModelAttribute("form") BatchCreateForm form,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            Long adminId = getAdminId(authentication);
            Batches updated = masterMaintenanceService.updateBatch(id, form, adminId);
            redirectAttributes.addFlashAttribute("success", "Batch '" + updated.getBchname() + "' updated successfully.");
            return "redirect:/admin/master-maintenance/batches/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/master-maintenance/batches/" + id + "/edit";
        }
    }

    @GetMapping("/batches/{id}/import")
    public String showImportForm(@PathVariable("id") Long id, Model model) {
        Batches batch = masterMaintenanceService.getBatchById(id);
        model.addAttribute("batch", batch);
        return "admin/mm_batch_import";
    }

    @PostMapping("/batches/{id}/import")
    public String processStudentImport(@PathVariable("id") Long id,
                                       @RequestParam("file") MultipartFile file,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            Long adminId = getAdminId(authentication);
            StudentImportResult result = masterMaintenanceService.importStudentsFromCsv(file, id, adminId);
            redirectAttributes.addFlashAttribute("importResult", result);
            if (result.getSuccessCount() > 0) {
                redirectAttributes.addFlashAttribute("success", "Import complete: Successfully imported " + result.getSuccessCount() + " student(s)!");
            }
            if (result.getErrorCount() > 0) {
                redirectAttributes.addFlashAttribute("warning", result.getErrorCount() + " row(s) had errors and were skipped.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to import students: " + ex.getMessage());
        }
        return "redirect:/admin/master-maintenance/batches/" + id + "/import";
    }

    // =========================================================================
    // Module 3: Any Non-Student User Management
    // =========================================================================

    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Users> usersList = masterMaintenanceService.getNonStudentUsers(search);
        model.addAttribute("usersList", usersList);
        model.addAttribute("search", search);
        return "admin/mm_users_list";
    }

    @GetMapping("/users/add")
    public String showAddUserForm(Model model) {
        UserCreateForm form = new UserCreateForm();
        form.setUid(masterMaintenanceService.getNextUid());
        List<Role> roles = masterMaintenanceService.getAvailableRolesForUser();

        model.addAttribute("form", form);
        model.addAttribute("roles", roles);
        return "admin/mm_users_add";
    }

    @PostMapping("/users/add")
    public String saveUser(@ModelAttribute("form") UserCreateForm form,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            Long adminId = getAdminId(authentication);
            Users user = masterMaintenanceService.createUser(form, adminId);
            redirectAttributes.addFlashAttribute("success", "User '" + user.getUfullname() + "' created successfully with default password 'Daiict@26'.");
            return "redirect:/admin/master-maintenance/users";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/admin/master-maintenance/users/add";
        }
    }

    // =========================================================================
    // Module 4: Role Master
    // =========================================================================

    @GetMapping("/roles")
    public String listRoles(Model model) {
        List<Role> roles = masterMaintenanceService.getAllRoles();
        model.addAttribute("roles", roles);
        return "admin/mm_roles_list";
    }

    @GetMapping("/roles/add")
    public String showAddRoleForm(Model model) {
        model.addAttribute("form", new RoleCreateForm());
        return "admin/mm_roles_add";
    }

    @PostMapping("/roles/add")
    public String saveRole(@ModelAttribute("form") RoleCreateForm form,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            Long adminId = getAdminId(authentication);
            Role role = masterMaintenanceService.createRole(form, adminId);
            redirectAttributes.addFlashAttribute("success", "Role '" + role.getRoleName() + "' (" + role.getRid() + ") created successfully.");
            return "redirect:/admin/master-maintenance/roles";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/admin/master-maintenance/roles/add";
        }
    }

    @GetMapping("/api/programs/{programId}/specializations")
    @ResponseBody
    public List<Map<String, Object>> getSpecializationsByProgram(@PathVariable("programId") Long programId) {
        List<Object[]> rows = schemeDetailsRepository.findSpecializationsByProgramId(programId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("splid", ((Number) r[0]).longValue());
            map.put("splname", r[1] != null ? (String) r[1] : "");
            map.put("spldesc", r[2] != null ? (String) r[2] : "");
            map.put("schemeId", r[3] != null ? ((Number) r[3]).longValue() : null);
            list.add(map);
        }
        return list;
    }

}

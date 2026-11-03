package com.ecampus.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ecampus.dto.master.BatchCreateForm;
import com.ecampus.dto.master.BatchMasterViewDTO;
import com.ecampus.dto.master.RoleCreateForm;
import com.ecampus.dto.master.StudentImportResult;
import com.ecampus.dto.master.UserCreateForm;
import com.ecampus.model.Batches;
import com.ecampus.model.Programs;
import com.ecampus.model.Role;
import com.ecampus.model.Students;
import com.ecampus.model.Users;
import com.ecampus.repository.BatchesRepository;
import com.ecampus.repository.ProgramsRepository;
import com.ecampus.repository.RoleRepository;
import com.ecampus.repository.StudentsRepository;
import com.ecampus.repository.UserRepository;
import com.ecampus.service.MasterMaintenanceService;
import com.ecampus.util.UserRoles;

@Service
public class MasterMaintenanceServiceImpl implements MasterMaintenanceService {

    private static final String DEFAULT_PASSWORD = "Daiict@26";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BatchesRepository batchesRepository;

    @Autowired
    private StudentsRepository studentsRepository;

    @Autowired
    private ProgramsRepository programsRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    
    // Module 1: Faculty User
    

    @Override
    public List<Users> getFacultyList(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return userRepository.searchFacultyByName(search.trim());
        }
        return userRepository.findAllFacultyList();
    }

    @Override
    public Long getNextUid() {
        return userRepository.findNextUid();
    }

    @Override
    @Transactional
    public Users createFacultyUser(UserCreateForm form, Long adminId) {
        form.setUrole(UserRoles.FACULTY); // "913"
        return saveNewUserInternal(form, adminId, true);
    }

    
    // Module 2: Batches & Student Batch Import
    

    @Override
    public List<BatchMasterViewDTO> getBatchesList(String search) {
        List<Object[]> rows;
        if (search != null && !search.trim().isEmpty()) {
            rows = batchesRepository.searchBatchesMasterDetails(search.trim());
        } else {
            rows = batchesRepository.getAllBatchesMasterDetails();
        }

        List<BatchMasterViewDTO> list = new ArrayList<>();
        for (Object[] r : rows) {
            Long bchid = ((Number) r[0]).longValue();
            String bchname = (String) r[1];
            String prgname = (String) r[2];
            String splname = (String) r[3];
            Long bchcapacity = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            String bchinstcode = (String) r[5];
            Long studentCount = r[6] != null ? ((Number) r[6]).longValue() : 0L;

            list.add(new BatchMasterViewDTO(bchid, bchname, prgname, splname, bchcapacity, bchinstcode, studentCount));
        }
        return list;
    }

    @Override
    public Batches getBatchById(Long bchid) {
        return batchesRepository.findById(bchid)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found for ID: " + bchid));
    }

    @Override
    public Long getNextBatchId() {
        return batchesRepository.findNextBatchId();
    }

    @Override
    @Transactional
    public Batches createBatch(BatchCreateForm form, Long adminId) {
        if (form.getBchprgid() == null) {
            throw new IllegalArgumentException("Program is required.");
        }
        if (form.getBchname() == null || form.getBchname().trim().isEmpty()) {
            throw new IllegalArgumentException("Batch name is required.");
        }
        if (form.getBchcapacity() == null || form.getBchcapacity() <= 0) {
            throw new IllegalArgumentException("Valid batch capacity is required.");
        }

        Programs program = programsRepository.findById(form.getBchprgid())
                .orElseThrow(() -> new IllegalArgumentException("Selected program does not exist."));

        if (batchesRepository.existsByBchprgidAndBchname(form.getBchprgid(), form.getBchname().trim())) {
            throw new IllegalArgumentException("Batch with name '" + form.getBchname().trim() + "' already exists for this program.");
        }

        Long nextId = form.getBchid() != null && form.getBchid() > 0 ? form.getBchid() : batchesRepository.findNextBatchId();

        Batches batch = new Batches();
        batch.setBchid(nextId);
        batch.setPrograms(program);
        batch.setBchprgid(form.getBchprgid());
        batch.setBchname(form.getBchname().trim());
        batch.setBchcapacity(form.getBchcapacity());
        batch.setBchinstcode(form.getBchinstcode() != null ? form.getBchinstcode().trim() : "DAU");
        batch.setSchemeId(form.getSchemeId());
        batch.setSplid(form.getSplid());
        batch.setBchcalid(0L); // Skipped as instructed, default 0L to satisfy DB NOT NULL constraint
        batch.setBchrowstate(1L);
        batch.setBchcreatedby(adminId != null ? adminId : 0L);
        batch.setBchcreatedat(LocalDateTime.now());
        batch.setBchlastupdatedby(adminId != null ? adminId : 0L);
        batch.setBchlastupdatedat(LocalDateTime.now());

        return batchesRepository.save(batch);
    }

    @Override
    @Transactional
    public Batches updateBatch(Long bchid, BatchCreateForm form, Long adminId) {
        Batches existing = getBatchById(bchid);

        if (form.getBchname() == null || form.getBchname().trim().isEmpty()) {
            throw new IllegalArgumentException("Batch name is required.");
        }
        if (form.getBchcapacity() == null || form.getBchcapacity() <= 0) {
            throw new IllegalArgumentException("Valid batch capacity is required.");
        }

        existing.setBchname(form.getBchname().trim());
        existing.setBchcapacity(form.getBchcapacity());
        if (form.getBchinstcode() != null) {
            existing.setBchinstcode(form.getBchinstcode().trim());
        }
        existing.setBchlastupdatedby(adminId != null ? adminId : 0L);
        existing.setBchlastupdatedat(LocalDateTime.now());

        return batchesRepository.save(existing);
    }

    @Override
    public List<Students> getStudentsInBatch(Long bchid) {
        return studentsRepository.findActiveStudentsByBatchId(bchid);
    }

    @Override
    @Transactional
    public StudentImportResult importStudentsFromCsv(MultipartFile file, Long bchid, Long adminId) {
        Batches batch = getBatchById(bchid);
        StudentImportResult result = new StudentImportResult();

        if (file == null || file.isEmpty()) {
            result.addError("Please select a non-empty CSV file to upload.");
            result.setErrorCount(1);
            return result;
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv")) {
            result.addError("Uploaded file is not a CSV. Please upload a .csv file.");
            result.setErrorCount(1);
            return result;
        }

        Set<String> seenInstIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        Long currentNextStdId = studentsRepository.findNextStudentId();
        Long currentNextUid = userRepository.findNextUid();

        int rowNum = 1; // 1 is header

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : parser) {
                rowNum++;
                result.setTotalRows(result.getTotalRows() + 1);

                String instId = getCol(record, "stdinstid", "studentid", "student_id", "instid", "id");
                String firstName = getCol(record, "stdfirstname", "firstname", "first_name", "fname");
                String middleName = getCol(record, "stdmiddlename", "middlename", "middle_name", "mname");
                String lastName = getCol(record, "stdlastname", "lastname", "last_name", "lname");
                String email = getCol(record, "stdemail", "email", "uemail", "mail");
                String gender = getCol(record, "stdgender", "gender", "sex");
                String dobStr = getCol(record, "stddob", "dob", "birthdate", "dateofbirth");

                // Validation checks
                if (instId == null || instId.trim().isEmpty()) {
                    result.addError("Row " + rowNum + ": Student Institute ID (stdinstid) is missing.");
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }
                instId = instId.trim();

                if (firstName == null || firstName.trim().isEmpty()) {
                    result.addError("Row " + rowNum + ": Student First Name is missing for ID " + instId);
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }
                firstName = firstName.trim();

                if (lastName == null || lastName.trim().isEmpty()) {
                    result.addError("Row " + rowNum + ": Student Last Name is missing for ID " + instId);
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }
                lastName = lastName.trim();

                if (middleName == null) {
                    middleName = "";
                } else {
                    middleName = middleName.trim();
                }

                if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
                    result.addError("Row " + rowNum + ": Invalid or missing Email for student ID " + instId);
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }
                email = email.trim();

                // Normalize Gender
                if (gender != null && !gender.trim().isEmpty()) {
                    gender = gender.trim().toUpperCase(Locale.ROOT);
                    if (!gender.equals("M") && !gender.equals("F") && !gender.equals("O")) {
                        gender = gender.startsWith("M") ? "M" : (gender.startsWith("F") ? "F" : "O");
                    }
                } else {
                    gender = "M"; // default fallback
                }

                // Parse Date of Birth
                LocalDate dob = parseDob(dobStr);
                if (dobStr != null && !dobStr.trim().isEmpty() && dob == null) {
                    result.addError("Row " + rowNum + ": Invalid Date of Birth format '" + dobStr + "' for student ID " + instId + " (expected YYYY-MM-DD or DD-MM-YYYY)");
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }

                // Check duplicates within CSV
                if (!seenInstIds.add(instId.toLowerCase(Locale.ROOT))) {
                    result.addError("Row " + rowNum + ": Duplicate Student ID '" + instId + "' found within this CSV file.");
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }
                if (!seenEmails.add(email.toLowerCase(Locale.ROOT))) {
                    result.addError("Row " + rowNum + ": Duplicate Email '" + email + "' found within this CSV file.");
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }

                // Check DB uniqueness
                if (studentsRepository.existsByStdinstidIgnoreCase(instId) || userRepository.existsByUnivIdIgnoreCase(instId) || userRepository.existsByUnameIgnoreCase(instId)) {
                    result.addError("Row " + rowNum + ": Student ID '" + instId + "' already exists in database.");
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }

                if (studentsRepository.existsByStdemailIgnoreCase(email) || userRepository.existsByUemailIgnoreCase(email)) {
                    result.addError("Row " + rowNum + ": Email '" + email + "' already exists in database.");
                    result.setErrorCount(result.getErrorCount() + 1);
                    continue;
                }

                // Create Student entity
                Students student = new Students();
                student.setStdid(currentNextStdId);
                student.setStdbchid(batch.getBchid());
                student.setBatch(batch);
                student.setStdinstid(instId);
                student.setStdfirstname(firstName);
                student.setStdmiddlename(middleName);
                student.setStdlastname(lastName);
                student.setStdemail(email);
                student.setStdgender(gender);
                student.setStddob(dob);
                student.setStdstatus("A");
                student.setStdrowstate(1L);
                student.setStdcreatedby(adminId != null ? adminId : 0L);
                student.setStdcreatedat(LocalDateTime.now());
                student.setStdlastupdatedby(adminId != null ? adminId : 0L);
                student.setStdlastupdatedat(LocalDateTime.now());
                studentsRepository.save(student);

                // Create corresponding Student User entity
                Users user = new Users();
                user.setUid(currentNextUid);
                user.setStdid(currentNextStdId);
                user.setUnivId(instId);
                user.setUname(instId);
                String fullName = firstName + (middleName.isEmpty() ? "" : " " + middleName) + " " + lastName;
                user.setUfullname(fullName);
                user.setUrole(UserRoles.STUDENT); // "914"
                user.setUemail(email);
                user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                user.setStatus(true);
                user.setRowState((short) 1);
                user.setCreatedBy(adminId != null ? adminId : 0L);
                user.setCreateTs(LocalDateTime.now());
                userRepository.save(user);

                currentNextStdId++;
                currentNextUid++;
                result.setSuccessCount(result.getSuccessCount() + 1);
                result.addSuccess(instId + " - " + fullName);
            }
        } catch (Exception e) {
            result.addError("Error reading CSV file: " + e.getMessage());
            result.setErrorCount(result.getErrorCount() + 1);
        }

        return result;
    }

    
    // Module 3: Any User Management
    

    @Override
    public List<Users> getNonStudentUsers(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return userRepository.searchNonStudentUsers(search.trim());
        }
        return userRepository.findAllNonStudentUsers();
    }

    @Override
    public List<Role> getAvailableRolesForUser() {
        // Exclude Student role "914" as requested
        return roleRepository.findByRidNotOrderByRoleNameAsc(UserRoles.STUDENT);
    }

    @Override
    @Transactional
    public Users createUser(UserCreateForm form, Long adminId) {
        if (form.getUrole() == null || form.getUrole().trim().isEmpty()) {
            throw new IllegalArgumentException("User role is required.");
        }
        if (UserRoles.STUDENT.equals(form.getUrole().trim())) {
            throw new IllegalArgumentException("Student users cannot be created directly here. Use Batch Import instead.");
        }
        if (!roleRepository.existsByRid(form.getUrole().trim())) {
            throw new IllegalArgumentException("Invalid role selected: " + form.getUrole());
        }

        return saveNewUserInternal(form, adminId, false);
    }

    
    // Module 4: Role Master
    

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAllByOrderByRoleNameAsc();
    }

    @Override
    @Transactional
    public Role createRole(RoleCreateForm form, Long adminId) {
        if (form.getRid() == null || form.getRid().trim().isEmpty()) {
            throw new IllegalArgumentException("Role ID (Code) is required.");
        }
        if (form.getRoleName() == null || form.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role Name is required.");
        }

        String rid = form.getRid().trim();
        String roleName = form.getRoleName().trim();

        if (roleRepository.existsByRid(rid)) {
            throw new IllegalArgumentException("Role ID '" + rid + "' already exists.");
        }
        if (roleRepository.existsByRoleNameIgnoreCase(roleName)) {
            throw new IllegalArgumentException("Role Name '" + roleName + "' already exists.");
        }

        Role role = new Role();
        role.setRid(rid);
        role.setRoleName(roleName);
        role.setComments(form.getComments() != null ? form.getComments().trim() : null);
        role.setCreatedBy(adminId != null ? adminId : 0L);
        role.setCreateTs(LocalDateTime.now());
        role.setRowState((short) 1);

        return roleRepository.save(role);
    }

    
    // Helper Methods
    

    private Users saveNewUserInternal(UserCreateForm form, Long adminId, boolean isFaculty) {
        if (form.getUnivId() == null || form.getUnivId().trim().isEmpty()) {
            throw new IllegalArgumentException("University ID is required.");
        }
        if (form.getUname() == null || form.getUname().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (form.getUfullname() == null || form.getUfullname().trim().isEmpty()) {
            throw new IllegalArgumentException("Full Name is required.");
        }
        if (form.getUemail() == null || form.getUemail().trim().isEmpty() || !EMAIL_PATTERN.matcher(form.getUemail().trim()).matches()) {
            throw new IllegalArgumentException("A valid Email address is required.");
        }

        String univId = form.getUnivId().trim();
        String uname = form.getUname().trim();
        String ufullname = form.getUfullname().trim();
        String uemail = form.getUemail().trim();
        String urole = form.getUrole().trim();

        if (userRepository.existsByUnivIdIgnoreCase(univId)) {
            throw new IllegalArgumentException("University ID '" + univId + "' already exists.");
        }
        if (userRepository.existsByUnameIgnoreCase(uname)) {
            throw new IllegalArgumentException("Username '" + uname + "' already exists.");
        }
        if (userRepository.existsByUemailIgnoreCase(uemail)) {
            throw new IllegalArgumentException("Email '" + uemail + "' already exists.");
        }

        Long uid = (form.getUid() != null && form.getUid() > 0) ? form.getUid() : userRepository.findNextUid();
        if (userRepository.existsByUid(uid)) {
            uid = userRepository.findNextUid();
        }

        Users user = new Users();
        user.setUid(uid);
        user.setUnivId(univId);
        user.setUname(uname);
        user.setUfullname(ufullname);
        user.setUrole(urole);
        user.setUemail(uemail);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(true);
        user.setRowState((short) 1);
        user.setCreatedBy(adminId != null ? adminId : 0L);
        user.setCreateTs(LocalDateTime.now());

        return userRepository.save(user);
    }

    private String getCol(CSVRecord record, String... headerNames) {
        for (String name : headerNames) {
            if (record.isMapped(name)) {
                String val = record.get(name);
                if (val != null) return val;
            }
        }
        return null;
    }

    private LocalDate parseDob(String dobStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) {
            return null;
        }
        String clean = dobStr.trim();
        String[] patterns = {"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "yyyy/MM/dd", "d-M-yyyy", "d/M/yyyy"};
        for (String p : patterns) {
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        return null;
    }
}


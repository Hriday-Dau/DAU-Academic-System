package com.ecampus.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ecampus.dto.master.BatchCreateForm;
import com.ecampus.dto.master.BatchMasterViewDTO;
import com.ecampus.dto.master.RoleCreateForm;
import com.ecampus.dto.master.StudentImportResult;
import com.ecampus.dto.master.UserCreateForm;
import com.ecampus.model.Batches;
import com.ecampus.model.Role;
import com.ecampus.model.Students;
import com.ecampus.model.Users;

public interface MasterMaintenanceService {

    // --- Module 1: Faculty User ---
    List<Users> getFacultyList(String search);

    Long getNextUid();

    Users createFacultyUser(UserCreateForm form, Long adminId);

    // --- Module 2: Batches & Student Import ---
    List<BatchMasterViewDTO> getBatchesList(String search);

    Batches getBatchById(Long bchid);

    Long getNextBatchId();

    Batches createBatch(BatchCreateForm form, Long adminId);

    Batches updateBatch(Long bchid, BatchCreateForm form, Long adminId);

    List<Students> getStudentsInBatch(Long bchid);

    StudentImportResult importStudentsFromCsv(MultipartFile file, Long bchid, Long adminId);

    // --- Module 3: Any Non-Student User ---
    List<Users> getNonStudentUsers(String search);

    List<Role> getAvailableRolesForUser();

    Users createUser(UserCreateForm form, Long adminId);

    // --- Module 4: Role Master ---
    List<Role> getAllRoles();

    Role createRole(RoleCreateForm form, Long adminId);
}


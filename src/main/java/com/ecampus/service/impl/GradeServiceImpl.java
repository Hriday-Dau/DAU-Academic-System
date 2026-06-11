package com.ecampus.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecampus.model.Egcrstt1;
import com.ecampus.model.Egcrstt1Id;
import com.ecampus.model.Eggradm1;
import com.ecampus.model.Grade;
import com.ecampus.model.GradeChangeStatusDTO;
import com.ecampus.model.GradeModRequestDetails;
import com.ecampus.model.GradeModRequests;
import com.ecampus.model.StudentGradeDTO;
import com.ecampus.model.Students;
import com.ecampus.model.TermCourses;
import com.ecampus.model.WorkTrail;
import com.ecampus.repository.CoursesRepository;
import com.ecampus.repository.Egcrstt1Repository;
import com.ecampus.repository.Eggradm1Repository;
import com.ecampus.repository.GradeModRequestDetailsRepository;
import com.ecampus.repository.GradeModRequestsRepository;
import com.ecampus.repository.StudentsRepository;
import com.ecampus.repository.TermCoursesRepository;
import com.ecampus.repository.TermsRepository;
import com.ecampus.repository.WorkTrailRepository;
import com.ecampus.service.GradeModificationService;
import com.ecampus.service.GradeService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private Eggradm1Repository eggradm1Repository;

    @Autowired
    private TermCoursesRepository termCoursesRepository;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private CoursesRepository coursesRepository;

    @Autowired
    private StudentsRepository studentsRepository;

    @Autowired
    private GradeModRequestsRepository gradeModRequestsRepository;

    @Autowired
    private GradeModRequestDetailsRepository gradeModRequestDetailsRepository;

    @Autowired
    private WorkTrailRepository workTrailRepository;

    @Autowired
    private Egcrstt1Repository egcrstt1Repository;

    @Autowired
    private GradeModificationService gradeModificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Grade> getAllGrades() {
        return eggradm1Repository.findActiveGrades().stream()
                .map(Eggradm1::getGradLt)
                .filter(value -> value != null && !value.isBlank())
                .map(Grade::new)
                .toList();
    }

    @Override
    public List<StudentGradeDTO> getStudentGrades(Long crsid, Long trmid, Long examTypeId, List<String> selectedGrades) {
        Long tcrid = termCoursesRepository.findTcridByCrsidAndTrmid(crsid, trmid);
        if (tcrid == null) {
            return new ArrayList<>();
        }

        StringBuilder existingSql = new StringBuilder("""
                SELECT s.stdinstid,
                       CONCAT_WS(' ', s.stdfirstname, s.stdmiddlename, s.stdlastname) AS student_name,
                       s.stdemail,
                       g.grad_lt
                FROM ec2.egcrstt1 e
                JOIN ec2.students s ON e.stud_id = s.stdid
                JOIN ec2.eggradm1 g ON e.obtgr_id = g.grad_id
                WHERE e.tcrid = :tcrid
                  AND e.examtype_id = :examTypeId
                  AND e.row_st > '0'
                """);

        if (selectedGrades != null && !selectedGrades.isEmpty()) {
            existingSql.append(" AND g.grad_lt IN (:selectedGrades)");
        }

        Query existingQuery = entityManager.createNativeQuery(existingSql.toString());
        existingQuery.setParameter("tcrid", tcrid);
        existingQuery.setParameter("examTypeId", examTypeId);
        if (selectedGrades != null && !selectedGrades.isEmpty()) {
            existingQuery.setParameter("selectedGrades", selectedGrades);
        }

        List<StudentGradeDTO> grades = new ArrayList<>();
        for (Object[] row : rows(existingQuery)) {
            grades.add(new StudentGradeDTO(asString(row[0]), asString(row[1]), asString(row[2]), asString(row[3])));
        }

        String missingSql = """
                SELECT s.stdinstid,
                       CONCAT_WS(' ', s.stdfirstname, s.stdmiddlename, s.stdlastname) AS student_name,
                       s.stdemail
                FROM ec2.studentregistrationcourses src
                JOIN ec2.studentregistrations sr ON src.srcsrgid = sr.srgid
                JOIN ec2.students s ON sr.srgstdid = s.stdid
                LEFT JOIN ec2.egcrstt1 e
                  ON e.stud_id = sr.srgstdid
                 AND e.tcrid = :tcrid
                 AND e.examtype_id = :examTypeId
                WHERE src.srctcrid = :tcrid
                  AND src.srcrowstate > 0
                  AND sr.srgrowstate > 0
                  AND e.stud_id IS NULL
                """;

        Query missingQuery = entityManager.createNativeQuery(missingSql);
        missingQuery.setParameter("tcrid", tcrid);
        missingQuery.setParameter("examTypeId", examTypeId);
        for (Object[] row : rows(missingQuery)) {
            grades.add(new StudentGradeDTO(asString(row[0]), asString(row[1]), asString(row[2]), "NULL"));
        }

        return grades;
    }

    @Transactional
    @Override
    public void saveOrUpdateGrades(List<StudentGradeDTO> gradesList, Long tcrid, Long examTypeId) {
        if (gradesList == null || gradesList.isEmpty()) {
            return;
        }

        TermCourses termCourse = termCoursesRepository.findById(tcrid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid term-course id: " + tcrid));
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        for (StudentGradeDTO dto : gradesList) {
            String gradeValue = firstNonBlank(dto.getModifiedGrade(), dto.getGrade());
            if (gradeValue == null || "NULL".equalsIgnoreCase(gradeValue)) {
                continue;
            }

            List<Long> studentIds = studentsRepository.findStudentIdByInstituteId(dto.getStudentId());
            if (studentIds.isEmpty()) {
                continue;
            }

            Long gradeId = eggradm1Repository.findGradeIdByValue(gradeValue);
            BigDecimal gradePoint = eggradm1Repository.findGradePointByValue(gradeValue);
            if (gradeId == null) {
                continue;
            }

            for (Long studentId : studentIds) {
                int updated = entityManager.createNativeQuery("""
                        UPDATE ec2.egcrstt1
                           SET obtgr_id = :gradeId,
                               obt_credits = :obtCredits,
                               updat_by = :updatedBy,
                               updat_dt = :updatedAt,
                               row_st = '1'
                         WHERE stud_id = :studentId
                           AND tcrid = :tcrid
                           AND examtype_id = :examTypeId
                        """)
                        .setParameter("gradeId", gradeId)
                        .setParameter("obtCredits", gradePoint)
                        .setParameter("updatedBy", 7L)
                        .setParameter("updatedAt", now)
                        .setParameter("studentId", studentId)
                        .setParameter("tcrid", tcrid)
                        .setParameter("examTypeId", examTypeId)
                        .executeUpdate();

                if (updated == 0) {
                    entityManager.createNativeQuery("""
                            INSERT INTO ec2.egcrstt1
                                (stud_id, tcrid, examtype_id, obtgr_id, obt_mks, obt_credits,
                                 crst_field1, creat_by, creat_dt, updat_by, updat_dt, row_st, crsid)
                            VALUES
                                (:studentId, :tcrid, :examTypeId, :gradeId, NULL, :obtCredits,
                                 NULL, :createdBy, :createdAt, :updatedBy, :updatedAt, '1', :crsid)
                            """)
                            .setParameter("studentId", studentId)
                            .setParameter("tcrid", tcrid)
                            .setParameter("examTypeId", examTypeId)
                            .setParameter("gradeId", gradeId)
                            .setParameter("obtCredits", gradePoint)
                            .setParameter("createdBy", 7L)
                            .setParameter("createdAt", now)
                            .setParameter("updatedBy", 7L)
                            .setParameter("updatedAt", now)
                            .setParameter("crsid", termCourse.getTcrcrsid())
                            .executeUpdate();
                }
            }
        }
    }

    @Transactional
    @Override
    public Long createGradeModificationRequest(Long facultyId, Long tcrid, Long examTypeId, String requestDesc,
            List<StudentGradeDTO> studentGrades) {
        List<StudentGradeDTO> selected = studentGrades == null ? List.of()
                : studentGrades.stream().filter(StudentGradeDTO::isSelectedForUpdate).toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("At least one student must be selected.");
        }

        Long requestId = nextWorkId();

        GradeModRequests request = new GradeModRequests();
        request.setGmdReqId(requestId);
        request.setGmdTcrId(tcrid.intValue());
        request.setGmdReqDesc(firstNonBlank(requestDesc, "Grade modification request"));
        request.setGmdCreatedBy(facultyId);
        request.setGmdCreatedAt(LocalDateTime.now());
        request.setGmdApprovalStatus("Pending Dean Approval");
        request.setGmdExamTypeId(examTypeId);
        request.setGmdReqRowState((short) 1);
        request.setGmdIterationNo((short) 1);
        gradeModRequestsRepository.save(request);

        WorkTrail trail = new WorkTrail();
        trail.setWorkId(requestId);
        trail.setWorkTypeCode(28L);
        trail.setNodeNumber(0);
        trail.setIterationNumber(1);
        trail.setEmployeeId(facultyId);
        trail.setResponseCode(1);
        trail.setResponseDate(LocalDateTime.now());
        trail.setRemarks(request.getGmdReqDesc());
        workTrailRepository.save(trail);

        for (StudentGradeDTO dto : selected) {
            List<Long> studentIds = studentsRepository.findStudentIdByInstituteId(dto.getStudentId());
            if (studentIds.isEmpty()) {
                throw new IllegalArgumentException("Student not found: " + dto.getStudentId());
            }

            Long presentGradeId = eggradm1Repository.findGradeIdByValue(dto.getGrade());
            Long newGradeId = eggradm1Repository.findGradeIdByValue(dto.getModifiedGrade());
            if (presentGradeId == null || newGradeId == null) {
                throw new IllegalArgumentException("Invalid grade for student: " + dto.getStudentId());
            }

            GradeModRequestDetails detail = new GradeModRequestDetails();
            detail.setGmdId(requestId);
            detail.setGmdStdId(studentIds.get(0));
            detail.setGmdPresentGrade(presentGradeId);
            detail.setGmdNewGrade(newGradeId);
            detail.setGmdChangeDesc(dto.getRemarks());
            detail.setGmdRowState((short) 1);
            gradeModRequestDetailsRepository.save(detail);
        }

        return requestId;
    }

    @Override
    public Map<String, Long> getGradeDistribution(Long crsid, Long trmid, Long examTypeId) {
        List<StudentGradeDTO> grades = getStudentGrades(crsid, trmid, examTypeId,
                List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F", "I"));
        if (grades.isEmpty()) {
            return new HashMap<>();
        }
        return grades.stream()
                .filter(dto -> dto.getGrade() != null && !"NULL".equalsIgnoreCase(dto.getGrade()))
                .collect(Collectors.groupingBy(StudentGradeDTO::getGrade, TreeMap::new, Collectors.counting()));
    }

    @Override
    public String getTermName(Long termId) {
        return termsRepository.findById(termId).map(term -> term.getTrmname()).orElse("Unknown Term");
    }

    @Override
    public String getCourseName(Long courseId) {
        return coursesRepository.findById(courseId).map(course -> course.getCrsname()).orElse("Unknown Course");
    }

    @Override
    public List<GradeChangeStatusDTO> getGradeChangeStatuses(Long facultyId, String facultyUsername, Long tcrid) {
        return gradeModRequestsRepository.findByGmdCreatedByAndGmdTcrId(facultyId, tcrid.intValue()).stream()
                .flatMap(request -> toStatusDtos(request).stream())
                .toList();
    }

    @Override
    public List<GradeChangeStatusDTO> getPendingDeanRequests() {
        return gradeModRequestsRepository.findByGmdApprovalStatus("Pending Dean Approval").stream()
                .flatMap(request -> toStatusDtos(request).stream())
                .toList();
    }

    @Override
    public List<GradeChangeStatusDTO> getPendingRegistrarRequests() {
        return gradeModRequestsRepository.findByGmdApprovalStatus("Pending Registrar Approval").stream()
                .flatMap(request -> toStatusDtos(request).stream())
                .toList();
    }

    @Override
    public boolean processDeanAction(Long gmdid, Long deanId, String action) {
        try {
            gradeModificationService.processDeanAction(gmdid, normalizedAction(action), null, deanId);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public boolean processRegistrarAction(Long gmdid, Long registrarId, String action) {
        try {
            gradeModificationService.processRegistrarAction(gmdid, normalizedAction(action), null, registrarId);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public boolean checkIfGradesExist(Long crsid, Long trmid, Long examTypeId) {
        return getStudentGrades(crsid, trmid, examTypeId, List.of("AA", "AB", "BB", "BC", "CC", "CD", "DD", "F", "I"))
                .stream()
                .anyMatch(dto -> dto.getGrade() != null && !"NULL".equalsIgnoreCase(dto.getGrade()));
    }

    private List<GradeChangeStatusDTO> toStatusDtos(GradeModRequests request) {
        List<GradeModRequestDetails> details = gradeModRequestDetailsRepository.findByGmdId(request.getGmdReqId());
        List<GradeChangeStatusDTO> dtos = new ArrayList<>();

        for (GradeModRequestDetails detail : details) {
            GradeChangeStatusDTO dto = new GradeChangeStatusDTO();
            Students student = studentsRepository.findStudent(detail.getGmdStdId());
            dto.setStudentInstituteId(student == null ? "Unknown" : student.getStdinstid());
            dto.setGmdid(request.getGmdReqId());
            dto.setPresentGradeLetter(eggradm1Repository.getGrade(detail.getGmdPresentGrade()));
            dto.setNewGradeLetter(eggradm1Repository.getGrade(detail.getGmdNewGrade()));
            dto.setRemarks(detail.getGmdChangeDesc());
            setStatusesFromApproval(request.getGmdApprovalStatus(), dto);
            dtos.add(dto);
        }
        return dtos;
    }

    private void setStatusesFromApproval(String approvalStatus, GradeChangeStatusDTO dto) {
        dto.setStatusSubmitted("Submitted Successfully");
        switch (approvalStatus) {
            case "Rejected by Dean" -> {
                dto.setStatusDean("Rejected");
                dto.setStatusRegistrar("Pending");
            }
            case "Pending Registrar Approval" -> {
                dto.setStatusDean("Approved");
                dto.setStatusRegistrar("Pending");
            }
            case "Rejected by Registrar" -> {
                dto.setStatusDean("Approved");
                dto.setStatusRegistrar("Rejected");
            }
            case "Approved" -> {
                dto.setStatusDean("Approved");
                dto.setStatusRegistrar("Approved");
            }
            default -> {
                dto.setStatusDean("Pending");
                dto.setStatusRegistrar("Pending");
            }
        }
    }

    private Long nextWorkId() {
        Number max = (Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(work_id), 0) FROM ec2.work_trail")
                .getSingleResult();
        return max.longValue() + 1L;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(Query query) {
        return query.getResultList();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private String normalizedAction(String action) {
        return "approve".equalsIgnoreCase(action) || "approved".equalsIgnoreCase(action) ? "APPROVE" : "REJECT";
    }
}

package com.ecampus.service;

import java.util.List;
import java.util.Map;

import com.ecampus.model.Grade;
import com.ecampus.model.GradeChangeStatusDTO;
import com.ecampus.model.StudentGradeDTO;

public interface GradeService {

    List<Grade> getAllGrades();

    Map<String, Long> getGradeDistribution(Long crsid, Long trmid, Long examTypeId);

    List<StudentGradeDTO> getStudentGrades(Long crsid, Long trmid, Long examTypeId, List<String> selectedGrades);

    void saveOrUpdateGrades(List<StudentGradeDTO> gradesList, Long tcrid, Long examTypeId);

    Long createGradeModificationRequest(Long facultyId, Long tcrid, Long examTypeId, String requestDesc,
            List<StudentGradeDTO> studentGrades);

    String getTermName(Long termId);

    String getCourseName(Long courseId);

    List<GradeChangeStatusDTO> getGradeChangeStatuses(Long facultyId, String facultyUsername, Long tcrid);

    List<GradeChangeStatusDTO> getPendingDeanRequests();

    List<GradeChangeStatusDTO> getPendingRegistrarRequests();

    boolean processDeanAction(Long gmdid, Long deanId, String action);

    boolean processRegistrarAction(Long gmdid, Long registrarId, String action);

    boolean checkIfGradesExist(Long crsid, Long trmid, Long examTypeId);
}

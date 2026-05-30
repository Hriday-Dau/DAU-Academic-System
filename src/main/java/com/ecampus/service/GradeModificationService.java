package com.ecampus.service;

import com.ecampus.dto.*;
import com.ecampus.model.*;
import com.ecampus.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Transactional Import

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GradeModificationService {

    @Autowired
    private GradeModRequestsRepository repository;

    @Autowired
    private GradeModRequestDetailsRepository detailsRepository;

    @Autowired
    private TermCoursesRepository termCoursesRepository;

    @Autowired
    private WorkTrailRepository workTrailRepository;

    @Autowired
    private Egcrstt1Repository egcrstt1Repository; // for updating grade

    public List<GradeModSummaryDTO> getPendingRequestsForFaculty(Long facultyId) {
        return repository.findPendingByFaculty(facultyId);
    }

    public List<GradeModSummaryDTO> getCompletedRequestsForFaculty(Long facultyId) {
        return repository.findCompletedByFaculty(facultyId);
    }

    // Fetches the specific student details for a request
    public List<GradeModDetailDTO> getDetailedStudentList(Long requestId) {
        return detailsRepository.findDetailsByRequestId(requestId);
    }

    // fetches the main request header
    public GradeModRequests getRequestById(Long requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Grade Modification Request not found with ID: " + requestId));
    }

    public String getFormattedCourseInfo(Integer tcrId) {
        // Fetching the TermCourse entity using the ID from the request header
        return termCoursesRepository.findById(tcrId.longValue())
                .map(tc -> String.format("%s - %s, %s %s",
                        tc.getCourse().getCrscode(),
                        tc.getCourse().getCrsname(),
                        tc.getTerms().getTrmname(),
                        tc.getTerms().getAcademicYear().getAyrname()))
                .orElse("Unknown Course Details");
    }

    // Fetches all requests that are currently at the 'Pending Dean Approval' stage
    public List<GradeModAdminSummaryDTO> getRequestsForDean() {
        return repository.findRequestsForDean();
    }

    @Transactional
    public void processDeanAction(Long requestId, String action, String remarks, Long deanUid) {
        // -- Fetch the Request Header
        GradeModRequests request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // -- Creating Audit Trail Entry using our model fields
        WorkTrail trail = new WorkTrail();
        trail.setWorkId(requestId); //
        trail.setNodeNumber(1); // Dean is Node 1
        trail.setIterationNumber(1); // Default first attempt
        trail.setEmployeeId(deanUid);
        trail.setRemarks(remarks != null && !remarks.isEmpty() ? remarks : null);
        trail.setResponseDate(LocalDateTime.now());

        // workTypeCode = 28 for our Grade Modification
        trail.setWorkTypeCode(28L);

        // -- Determine New Status and responseCode based on Action
        if ("APPROVE".equalsIgnoreCase(action)) {
            request.setGmdApprovalStatus("Pending Registrar Approval");
            trail.setResponseCode(1);
        } else {
            request.setGmdApprovalStatus("Rejected by Dean");
            trail.setResponseCode(2);
        }

        // -- save transaction Changes
        repository.save(request);
        workTrailRepository.save(trail);
    }

    public List<GradeModAdminSummaryDTO> getRequestsForRegistrar() {
        return repository.findRequestsForRegistrar();
    }

    @Transactional
    public void processRegistrarAction(Long requestId, String action, String remarks, Long regUid) {
        GradeModRequests request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // -- Audit Trail Logic
        WorkTrail trail = new WorkTrail();
        trail.setWorkId(requestId);
        trail.setNodeNumber(2); // Registrar
        trail.setEmployeeId(regUid);
        trail.setRemarks(remarks);
        trail.setResponseDate(LocalDateTime.now());
        trail.setWorkTypeCode(28L);

        if ("APPROVE".equalsIgnoreCase(action)) {
            request.setGmdApprovalStatus("Approved");
            trail.setResponseCode(1); // Approved

            // -- Database Update for Student Grades
            List<GradeModRequestDetails> detailList = detailsRepository.findByGmdId(requestId);

            for (GradeModRequestDetails detail : detailList) {
                // Use Egcrstt1Id with Tcrid from request and StudId from detail
                Egcrstt1Id id = new Egcrstt1Id(request.getGmdTcrId().longValue(), detail.getGmdStdId());

                egcrstt1Repository.findById(id).ifPresent(record -> {
                    record.setObtgrId(detail.getGmdNewGrade()); // Update Grade ID
                    record.setUpdatBy(regUid); // Audit: Registrar
                    record.setUpdatDt(LocalDateTime.now()); // Audit: Current Time
                    egcrstt1Repository.save(record); // Commit change
                });
            }

        } else {
            request.setGmdApprovalStatus("Rejected by Registrar");
            trail.setResponseCode(2); // Rejected
        }

        repository.save(request);
        workTrailRepository.save(trail);
    }

}
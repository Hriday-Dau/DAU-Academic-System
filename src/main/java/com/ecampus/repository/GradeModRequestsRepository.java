package com.ecampus.repository;

import com.ecampus.model.GradeModRequests;
import com.ecampus.dto.GradeModAdminSummaryDTO;
import com.ecampus.dto.GradeModSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeModRequestsRepository extends JpaRepository<GradeModRequests, Long> {

       // requests for Dean/Registrar: SELECT * FROM grademodrequests WHERE
       // gmd_approvalstatus = ?
       List<GradeModRequests> findByGmdApprovalStatus(String status);

       // requests created by a specific Faculty
       List<GradeModRequests> findByGmdCreatedBy(Long userId);

       // Queries for user display

       // -- Fetch Pending reqeusts
       // remarks fetched from Node 1 if the request is with the Registrar
       @Query("SELECT new com.ecampus.dto.GradeModSummaryDTO(" +
                     "r.gmdReqId, c.crscode, c.crsname, " +
                     "CONCAT(c.crscode, ' - ', c.crsname, ', ', t.trmname, ' ', ay.ayrname), " +
                     "(SELECT wt.remarks FROM WorkTrail wt WHERE wt.workId = r.gmdReqId AND wt.nodeNumber = 1), " +
                     "r.gmdApprovalStatus, r.gmdCreatedAt) " +
                     "FROM GradeModRequests r " +
                     "JOIN TermCourses tc ON r.gmdTcrId = tc.tcrid " +
                     "JOIN Courses c ON tc.tcrcrsid = c.crsid " +
                     "JOIN Terms t ON tc.tcrtrmid = t.trmid " +
                     "JOIN AcademicYears ay ON t.trmayrid = ay.ayrid " +
                     "WHERE r.gmdCreatedBy = :facultyId " +
                     "AND r.gmdApprovalStatus IN ('Pending Dean Approval', 'Pending Registrar Approval')")
       List<GradeModSummaryDTO> findPendingByFaculty(@Param("facultyId") Long facultyId);

       // -- Fetch Completed (including Rejections)
       @Query("SELECT new com.ecampus.dto.GradeModSummaryDTO(" +
                     "r.gmdReqId, c.crscode, c.crsname, " +
                     "CONCAT(c.crscode, ' - ', c.crsname, ', ', t.trmname, ' ', ay.ayrname), " +
                     "(SELECT wt.remarks FROM WorkTrail wt WHERE wt.workId = r.gmdReqId AND wt.nodeNumber = " +
                     "(CASE r.gmdApprovalStatus WHEN 'Approved' THEN 2 WHEN 'Rejected by Registrar' THEN 2 ELSE 1 END)), "
                     +
                     "r.gmdApprovalStatus, r.gmdCreatedAt) " +
                     "FROM GradeModRequests r " +
                     "JOIN TermCourses tc ON r.gmdTcrId = tc.tcrid " +
                     "JOIN Courses c ON tc.tcrcrsid = c.crsid " +
                     "JOIN Terms t ON tc.tcrtrmid = t.trmid " +
                     "JOIN AcademicYears ay ON t.trmayrid = ay.ayrid " +
                     "WHERE r.gmdCreatedBy = :facultyId " +
                     "AND r.gmdApprovalStatus IN ('Approved', 'Rejected by Registrar', 'Rejected by Dean')")
       List<GradeModSummaryDTO> findCompletedByFaculty(@Param("facultyId") Long facultyId);

       @Query("SELECT new com.ecampus.dto.GradeModAdminSummaryDTO(" +
                     "r.gmdReqId, u.univId, u.ufullname, " +
                     "CONCAT(c.crscode, ' - ', c.crsname, ', ', t.trmname, ' ', ay.ayrname), " +
                     "r.gmdCreatedAt, r.gmdApprovalStatus, r.gmdReqDesc, '' ) " + // supply empty deanRemarks
                     "FROM GradeModRequests r " +
                     "JOIN Users u ON r.gmdCreatedBy = u.uid " +
                     "JOIN TermCourses tc ON r.gmdTcrId = tc.tcrid " +
                     "JOIN Courses c ON tc.tcrcrsid = c.crsid " +
                     "JOIN Terms t ON tc.tcrtrmid = t.trmid " +
                     "JOIN AcademicYears ay ON t.trmayrid = ay.ayrid " +
                     "WHERE r.gmdApprovalStatus = 'Pending Dean Approval'")
       List<GradeModAdminSummaryDTO> findRequestsForDean();

       @Query("SELECT new com.ecampus.dto.GradeModAdminSummaryDTO(" +
       "r.gmdReqId, u.univId, u.ufullname, " +
       "CONCAT(c.crscode, ' - ', c.crsname, ', ', t.trmname, ' ', ay.ayrname), " +
       "r.gmdCreatedAt, r.gmdApprovalStatus, r.gmdReqDesc, " +
       "(SELECT wt.remarks FROM WorkTrail wt WHERE wt.workId = r.gmdReqId AND wt.nodeNumber = 1)) " +
       "FROM GradeModRequests r " +
       "JOIN Users u ON r.gmdCreatedBy = u.uid " +
       "JOIN TermCourses tc ON r.gmdTcrId = tc.tcrid " +
       "JOIN Courses c ON tc.tcrcrsid = c.crsid " +
       "JOIN Terms t ON tc.tcrtrmid = t.trmid " +
       "JOIN AcademicYears ay ON t.trmayrid = ay.ayrid " +
       "WHERE r.gmdApprovalStatus = 'Pending Registrar Approval'")
List<GradeModAdminSummaryDTO> findRequestsForRegistrar();
}

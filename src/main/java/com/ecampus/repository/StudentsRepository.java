package com.ecampus.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecampus.model.Batches;
import com.ecampus.model.Students;

@Repository
public interface StudentsRepository extends JpaRepository<Students, Long> {

    // Native query for fetching one student by stdid
    @Query(value = "SELECT * FROM ec2.students st WHERE st.stdrowstate > 0 AND st.stdid = :studentId", nativeQuery = true)
    Students findStudent(@Param("studentId") Long studentId);

    // Latest stdid by institute id
    @Query(value = "SELECT st.stdid FROM ec2.students st WHERE st.stdinstid = :studentId ORDER BY st.stdrowstate DESC LIMIT 1", nativeQuery = true)
    Long findStdid(@Param("studentId") String studentId);

    // Batch id by institute id
    @Query(value = "SELECT st.stdbchid FROM ec2.students st WHERE st.stdinstid = :studentId ORDER BY st.stdrowstate DESC LIMIT 1", nativeQuery = true)
    Long findBatchIdByStudentId(@Param("studentId") String studentId);

    // Paging queries
    Page<Students> findByStdinstidAndStdrowstateGreaterThan(String stdinstid, int state, Pageable pageable);

    Page<Students> findByStdfirstnameContainingIgnoreCaseAndStdlastnameContainingIgnoreCaseAndStdrowstateGreaterThan(
            String fname, String lname, int state, Pageable pageable);

    Page<Students> findByStdfirstnameContainingIgnoreCaseAndStdrowstateGreaterThan(
            String fname, int state, Pageable pageable);

    Page<Students> findByStdlastnameContainingIgnoreCaseAndStdrowstateGreaterThan(
            String lname, int state, Pageable pageable);

    // Counts
    long countByStdfirstnameContainingIgnoreCaseAndStdrowstateGreaterThan(String stdfirstname, Long rowstate);

    long countByStdlastnameContainingIgnoreCaseAndStdrowstateGreaterThan(String stdlastname, Long rowstate);

    long countByStdfirstnameContainingIgnoreCaseAndStdlastnameContainingIgnoreCaseAndStdrowstateGreaterThan(
            String stdfirstname, String stdlastname, Long rowstate);

    // JPQL queries with StudentRegistrations will only work
    // if you actually mapped `@OneToMany` / `@ManyToOne` in Students.
    // Otherwise, keep them native.
    // Example fix (remove complex join if not mapped properly):
    @Query("SELECT s FROM Students s WHERE UPPER(s.stdfirstname) LIKE %:firstName% AND UPPER(s.stdlastname) LIKE %:lastName% AND s.stdrowstate > 0")
    List<Students> findStudentsByFullNameWithLatestRegistration(@Param("firstName") String firstName,
                                                                @Param("lastName") String lastName);

    @Query("SELECT s FROM Students s WHERE s.stdinstid = :stdinstid AND s.stdrowstate > 0")
    List<Students> findStudentByInstIdWithLatestRegistration(@Param("stdinstid") String stdinstid);

    // JPQL doesn’t allow LIMIT → Use native query
    @Query(value = "SELECT st.stdid FROM ec2.students st WHERE st.stdinstid = :stdinstid ORDER BY st.stdid ASC LIMIT 1", nativeQuery = true)
    List<Long> findStudentIdByInstituteId(@Param("stdinstid") String stdinstid);

    // Derived query by batch
    List<Students> findByBatch(Batches batches);

    List<Students> findByStdrowstateGreaterThanOrderByStdfirstnameAscStdlastnameAsc(int rowState);

    @Query(value = "SELECT s.stdinstid, " +
                "CONCAT(s.stdfirstname, ' ', s.stdmiddlename, ' ', s.stdlastname), " +
                "sd.splname, " +
                "sem.strname " +
                "FROM ec2.students s " +
                "JOIN ec2.batches b ON s.stdbchid = b.bchid " +
                "JOIN ec2.schemedetails sd ON b.scheme_id = sd.scheme_id AND b.splid = sd.splid " +
                "JOIN ec2.semesters sem ON b.bchid = sem.strbchid " +
                "WHERE b.bchname = :bchname " +
                "AND sd.splname = :splname " +
                "AND sem.strtrmid = :termId", nativeQuery = true)
        List<Object[]> findStudentRegistrationDetails(@Param("bchname") String bchname, 
                                                @Param("splname") String splname, 
                                                @Param("termId") Long termId);

    // ===================================================================
    // NEW QUERIES FOR FACULTY: STUDENT INFO MODULE
    // ===================================================================

    // -- Search by exactly Student Institute ID
    @Query("SELECT s FROM Students s " +
           "LEFT JOIN FETCH s.batch b " +
           "LEFT JOIN FETCH b.programs p " +
           "WHERE s.stdinstid = :stdinstid AND s.stdrowstate > 0")
    List<Students> getStudentInfoById(@Param("stdinstid") String stdinstid);

    // --search by Full Name (first + middle + last)
    @Query("SELECT s FROM Students s " +
           "LEFT JOIN FETCH s.batch b " +
           "LEFT JOIN FETCH b.programs p " +
           "WHERE LOWER(CONCAT(s.stdfirstname, ' ', COALESCE(s.stdmiddlename, ''), ' ', s.stdlastname)) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND s.stdrowstate > 0")
    List<Students> getStudentInfoByName(@Param("query") String query);

    // -- Native Query to fetch the student's latest registered Semester Name
    @Query(value = "SELECT sem.strname FROM ec2.studentregistrations sr " +
                   "JOIN ec2.semesters sem ON sr.srgstrid = sem.strid " +
                   "WHERE sr.srgstdid = :stdid " +
                   "ORDER BY sr.srgregdate DESC LIMIT 1", nativeQuery = true)
    String getLatestSemesterForStudent(@Param("stdid") Long stdid);
    
    // ===================================================================
    // QUERIES FOR TABS: REGISTRATION & RESULT
    // ===================================================================

    // Unified query fetching all registered courses AND their obtained grades
    @Query(value = "SELECT " +
            "sem.strid AS semesterId, " +
            "sem.strname AS semesterName, " +
            "c.crscode AS courseCode, " +
            "c.crstitle AS courseTitle, " +
            "ct.ctpname AS courseType, " +
            "c.crscreditpoints AS credits, " +
            "eg.grad_lt AS grade " +
            "FROM ec2.studentregistrations sr " +
            "JOIN ec2.semesters sem ON sr.srgstrid = sem.strid " +
            "JOIN ec2.studentregistrationcourses src ON src.srcsrgid = sr.srgid " +
            "JOIN ec2.termcourses tc ON src.srctcrid = tc.tcrid " +
            "JOIN ec2.courses c ON tc.tcrcrsid = c.crsid " +
            "LEFT JOIN ec2.coursetypes ct ON src.curr_ctpid = ct.ctpid " +
            "LEFT JOIN ec2.egcrstt1 exam ON exam.stud_id = sr.srgstdid AND exam.tcrid = tc.tcrid " +
            "LEFT JOIN ec2.eggradm1 eg ON exam.obtgr_id = eg.grad_id " +
            "WHERE sr.srgstdid = :stdid AND sr.srgrowstate > 0 AND src.srcrowstate > 0 " +
            "ORDER BY sem.strseqno ASC, c.crscode ASC", 
            nativeQuery = true)
    List<com.ecampus.dto.StudentAcademicRecordDTO> getStudentAcademicRecords(@Param("stdid") Long stdid);

    // Query to fetch the final SPI and CPI for each semester
    @Query(value = "SELECT " +
            "sem.strname AS semesterName, " +
            "ssr.ssrspi AS spi, " +
            "ssr.ssrcpi AS cpi " +
            "FROM ec2.studentsemesterresult ssr " +
            "JOIN ec2.studentregistrations sr ON ssr.ssrsrgid = sr.srgid " +
            "JOIN ec2.semesters sem ON sr.srgstrid = sem.strid " +
            "WHERE sr.srgstdid = :stdid AND sr.srgrowstate > 0 " +
            "ORDER BY sem.strseqno ASC", 
            nativeQuery = true)
    List<com.ecampus.dto.StudentSemesterResultDTO> getStudentSemesterResults(@Param("stdid") Long stdid);
}

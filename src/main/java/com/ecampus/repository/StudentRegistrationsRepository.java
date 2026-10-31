package com.ecampus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecampus.model.Semesters;
import com.ecampus.model.StudentRegistrations;

/**
 * Provides persistence operations for student registration and semester records.
 */
@Repository
public interface StudentRegistrationsRepository extends JpaRepository<StudentRegistrations, Long> {

    /**
     * Finds active registrations for a student.
     *
     * @param studentId the student database ID
     * @return the student's registered semesters
     */
    @Query(value = "SELECT * FROM ec2.STUDENTREGISTRATIONS srg WHERE srg.SRGROWSTATE > 0 AND srg.SRGSTDID=:studentId", nativeQuery = true)
    List<StudentRegistrations> findregisteredsemesters(@Param("studentId") Long studentId);

    /**
     * Finds an active registration for a student and semester.
     *
     * @param studentId the student database ID
     * @param semesterId the semester ID
     * @return the matching registration, or {@code null} if none exists
     */
    @Query(value = "SELECT * FROM ec2.STUDENTREGISTRATIONS srg WHERE srg.SRGROWSTATE > 0 AND srg.SRGSTDID=:studentId and srg.SRGSTRID=:semesterId", nativeQuery = true)
    StudentRegistrations getsrgbystdidandstrid(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /** @return the greatest registration ID, if any registrations exist */
    @Query("SELECT MAX(s.srgid) FROM StudentRegistrations s")
    Optional<Long> findMaxSrgid();

    /** @return active registrations ordered from newest to oldest */
    List<StudentRegistrations> findBysrgstdidAndSrgrowstateGreaterThanOrderBySrgidDesc(
            Long studentId, int state);

    /** @return the most recent registration, if one exists */
    Optional<StudentRegistrations> findTopBysrgstdidOrderBySrgidDesc(Long studentId);

    /** @return all registrations ordered by semester sequence */
    @Query("SELECT sr FROM StudentRegistrations sr JOIN sr.semesters s " +
            "WHERE sr.students.stdid = :studentId " +
            "ORDER BY s.strseqno ASC")
    List<StudentRegistrations> findAllRegistrationsByStudentIdOrderBySemesterSequence(
            @Param("studentId") Long studentId);

// Used for semester-wise grade card (corrected based on entity)
        /**
         * Finds an active registration for a student and semester.
         *
         * @param studentId the student database ID
         * @param semesterId the semester ID
         * @return the matching registration, or {@code null} if none exists
         */
    @Query("SELECT r FROM StudentRegistrations r " +
            "WHERE r.students.stdid = :studentId " +
            "AND r.semesters.strid = :semesterId " +
            "AND r.srgrowstate > 0")
    StudentRegistrations findByStudentIdAndSemesterId(@Param("studentId") Long studentId,
                                                     @Param("semesterId") Long semesterId);

// Semester Dropdown loading
        /**
         * Finds semesters belonging to a term.
         *
         * @param termId the term ID
         * @return semesters for the term
         */
    @Query(value = "SELECT * FROM ec2.semesters WHERE strtrmid = :termId", nativeQuery = true)
    List<Semesters> findSemestersByTerm(@Param("termId") Long termId);
    /** @return active registrations for the semester ordered by institute student ID */
    @Query("""
       SELECT r
       FROM StudentRegistrations r
       JOIN r.students s
       JOIN r.semesters sem
       WHERE sem.strid = :semesterId
         AND r.srgrowstate > 0
       ORDER BY s.stdinstid ASC
       """)
    List<StudentRegistrations> findBySemesterOrderByStudentInstId(@Param("semesterId") Long semesterId);

    /** @return registrations associated with the semester */
    @Query(value = "SELECT * FROM ec2.STUDENTREGISTRATIONS srg WHERE srg.SRGSTRID=:semesterId", nativeQuery = true)
    List<StudentRegistrations> findByStrid(@Param("semesterId") Long semesterId);

// === NEW QUERIES FOR FACULTY-SIDE: Student Info Module (Results Tab)===

        /**
         * Finds the latest registered semester name for a student.
         *
         * @param stdid the student database ID
         * @return the latest semester name
         */
    @Query(value = "SELECT sem.strname FROM ec2.studentregistrations sr " +
                   "JOIN ec2.semesters sem ON sr.srgstrid = sem.strid " +
                   "WHERE sr.srgstdid = :stdid " +
                   "ORDER BY sr.srgregdate DESC LIMIT 1", nativeQuery = true)
    String getLatestSemesterForStudent(@Param("stdid") Long stdid);

        /**
         * Fetches registered courses and obtained grades grouped by semester data.
         *
         * @param stdid the student database ID
         * @return the student's academic records
         */
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

}

package com.ecampus.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Important for dynamic queries
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecampus.model.StudentSemesterResult;

// toupdate

/**
 * Provides persistence operations for student semester results and SPI/CPI data.
 */
@Repository
public interface StudentSemesterResultRepository extends
        JpaRepository<StudentSemesterResult, Long>,
        JpaSpecificationExecutor<StudentSemesterResult>, // Keep this for standard Specification-based queries that return StudentSemesterResult entities
        CustomStudentSemesterResultRepository // Extend your custom interface for the SPI/CPI list
{

        // Corresponds to: SELECT * FROM STUDENTSEMESTERRESULT WHERE SSRROWSTATE > 0 AND SSRSRGID = ?
        /**
         * Finds active semester results for a registration.
         *
         * @param registrationId the registration ID
         * @param rowState the minimum row state
         * @return matching semester results
         */
    List<StudentSemesterResult> findByStudentRegistration_SrgidAndSsrrowstateGreaterThan(Long registrationId, short rowState);

        /**
         * Finds the latest CPI for a student.
         *
         * @param studentId the student database ID
         * @return the latest CPI
         */
        @Query(value = "SELECT ssr.SSRCPI FROM ec2.STUDENTSEMESTERRESULT ssr WHERE ssr.SSRSRGID in (SELECT srg.SRGID FROM ec2.STUDENTREGISTRATIONS srg WHERE srg.SRGSTDID=:studentId) ORDER BY ssr.SSRSRGID DESC LIMIT 1", nativeQuery = true)
        String getcpi(@Param("studentId") Long studentId);

        /**
         * Finds the latest semester result for a student.
         *
         * @param studentId the student database ID
         * @return the latest semester result, or {@code null} if none exists
         */
        @Query(value = "SELECT ssr.* FROM ec2.STUDENTSEMESTERRESULT ssr "
            + "JOIN ec2.STUDENTREGISTRATIONS srg ON ssr.SSRSRGID = srg.SRGID "
            + "WHERE srg.SRGSTDID = :studentId "
            + "ORDER BY ssr.SSRSRGID DESC LIMIT 1", nativeQuery = true)
        StudentSemesterResult findLatestByStudentId(@Param("studentId") Long studentId);

        /**
         * Finds the previous CPI for a student.
         *
         * @param studentId the student database ID
         * @return the previous CPI
         */
        @Query(value = "SELECT ssr.SSRCPI FROM ec2.STUDENTSEMESTERRESULT ssr WHERE ssr.SSRSRGID in (SELECT srg.SRGID FROM ec2.STUDENTREGISTRATIONS srg WHERE srg.SRGSTDID=:studentId) ORDER BY ssr.SSRSRGID DESC LIMIT 1 OFFSET 1", nativeQuery = true)
        String getlcpi(@Param("studentId") Long studentId);

        /**
         * Finds a semester result by registration ID.
         *
         * @param srgid the registration ID
         * @return the matching semester result
         */
    @Query(value = "SELECT * FROM ec2.STUDENTSEMESTERRESULT ssr WHERE ssr.SSRSRGID = :srgid", nativeQuery = true)
    StudentSemesterResult getBySrgid(@Param("srgid") Long srgid);

        /** @return the greatest semester result ID, or zero when none exists */
    @Query(value = "SELECT COALESCE(MAX(ssr.ssrid), 0) FROM ec2.STUDENTSEMESTERRESULT ssr", nativeQuery = true)
    Long findMaxId();

// === NEW QUERIES FOR FACULTY-SIDE: Student Info Module (Results Tab)===

        /**
         * Fetches SPI and CPI values for each semester of a student.
         *
         * @param stdid the student database ID
         * @return semester result summaries ordered by semester sequence
         */
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
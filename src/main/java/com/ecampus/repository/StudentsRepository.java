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

/**
 * Provides persistence operations for student records and faculty student information searches.
 */
@Repository
public interface StudentsRepository extends JpaRepository<Students, Long> {

    /**
     * Finds an active student by database ID.
     *
     * @param studentId the student database ID
     * @return the matching student, or {@code null} if none exists
     */
    @Query(value = "SELECT * FROM ec2.students st WHERE st.stdrowstate > 0 AND st.stdid = :studentId", nativeQuery = true)
    Students findStudent(@Param("studentId") Long studentId);

    /**
     * Finds the latest database student ID for an institute ID.
     *
     * @param studentId the institute student ID
     * @return the latest database student ID
     */
    @Query(value = "SELECT st.stdid FROM ec2.students st WHERE st.stdinstid = :studentId ORDER BY st.stdrowstate DESC LIMIT 1", nativeQuery = true)
    Long findStdid(@Param("studentId") String studentId);

    /**
     * Finds the batch ID associated with an institute student ID.
     *
     * @param studentId the institute student ID
     * @return the associated batch ID
     */
    @Query(value = "SELECT st.stdbchid FROM ec2.students st WHERE st.stdinstid = :studentId ORDER BY st.stdrowstate DESC LIMIT 1", nativeQuery = true)
    Long findBatchIdByStudentId(@Param("studentId") String studentId);

    /**
     * Finds active students by institute ID with pagination.
     *
     * @param stdinstid the institute student ID
     * @param state the minimum row state
     * @param pageable pagination and sorting information
     * @return a page of matching students
     */
    Page<Students> findByStdinstidAndStdrowstateGreaterThan(String stdinstid, int state, Pageable pageable);

    /**
     * Finds active students by first and last name with pagination.
     *
     * @param fname the first name fragment
     * @param lname the last name fragment
     * @param state the minimum row state
     * @param pageable pagination and sorting information
     * @return a page of matching students
     */
    Page<Students> findByStdfirstnameContainingIgnoreCaseAndStdlastnameContainingIgnoreCaseAndStdrowstateGreaterThan(
            String fname, String lname, int state, Pageable pageable);

    /**
     * Finds active students by first name with pagination.
     *
     * @param fname the first name fragment
     * @param state the minimum row state
     * @param pageable pagination and sorting information
     * @return a page of matching students
     */
    Page<Students> findByStdfirstnameContainingIgnoreCaseAndStdrowstateGreaterThan(
            String fname, int state, Pageable pageable);

    /**
     * Finds active students by last name with pagination.
     *
     * @param lname the last name fragment
     * @param state the minimum row state
     * @param pageable pagination and sorting information
     * @return a page of matching students
     */
    Page<Students> findByStdlastnameContainingIgnoreCaseAndStdrowstateGreaterThan(
            String lname, int state, Pageable pageable);

    /** @return the number of active students matching the first name */
    long countByStdfirstnameContainingIgnoreCaseAndStdrowstateGreaterThan(String stdfirstname, Long rowstate);

    /** @return the number of active students matching the last name */
    long countByStdlastnameContainingIgnoreCaseAndStdrowstateGreaterThan(String stdlastname, Long rowstate);

    /** @return the number of active students matching both names */
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

        /** @return students belonging to the supplied batch */
    List<Students> findByBatch(Batches batches);

        /** @return active students ordered by first name and last name */
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

// === NEW QUERIES FOR FACULTY-SIDE: Student Info Module ===

        /**
         * Searches for an active student by exact institute ID.
         *
         * @param stdinstid the institute student ID
         * @return matching students with batch and program data
         */
    @Query("SELECT s FROM Students s " +
           "LEFT JOIN FETCH s.batch b " +
           "LEFT JOIN FETCH b.programs p " +
           "WHERE s.stdinstid = :stdinstid AND s.stdrowstate > 0")
    List<Students> getStudentInfoById(@Param("stdinstid") String stdinstid);

        /**
         * Searches active students by their full name.
         *
         * @param query the full-name search text
         * @return matching students with batch and program data
         */
    @Query("SELECT s FROM Students s " +
           "LEFT JOIN FETCH s.batch b " +
           "LEFT JOIN FETCH b.programs p " +
           "WHERE LOWER(CONCAT(s.stdfirstname, ' ', COALESCE(s.stdmiddlename, ''), ' ', s.stdlastname)) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND s.stdrowstate > 0")
    List<Students> getStudentInfoByName(@Param("query") String query);


    @Query(value = "SELECT COALESCE(MAX(st.stdid), 0) + 1 FROM ec2.students st", nativeQuery = true)
    Long findNextStudentId();

    @Query(value = "SELECT * FROM ec2.students WHERE stdbchid = :bchid AND stdrowstate > 0 ORDER BY stdinstid ASC", nativeQuery = true)
    List<Students> findActiveStudentsByBatchId(@Param("bchid") Long bchid);

    @Query(value = "SELECT COUNT(1) > 0 FROM ec2.students WHERE LOWER(stdinstid) = LOWER(:stdinstid)", nativeQuery = true)
    boolean existsByStdinstidIgnoreCase(@Param("stdinstid") String stdinstid);

    @Query(value = "SELECT COUNT(1) > 0 FROM ec2.students WHERE LOWER(stdemail) = LOWER(:stdemail)", nativeQuery = true)
    boolean existsByStdemailIgnoreCase(@Param("stdemail") String stdemail);

}

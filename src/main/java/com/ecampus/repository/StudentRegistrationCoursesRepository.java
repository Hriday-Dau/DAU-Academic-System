package com.ecampus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecampus.model.StudentRegistrationCourses;

@Repository
public interface StudentRegistrationCoursesRepository extends JpaRepository<StudentRegistrationCourses, Long> {

    List<StudentRegistrationCourses> findBySrcsrgidIn(List<Long> srgIds);

    Optional<StudentRegistrationCourses> findBySrcid(Long srcid);
    
    // Find all courses for a specific student registration
    @Query(value = "SELECT * FROM ec2.STUDENTREGISTRATIONCOURSES src WHERE src.SRCSRGID = :srgid AND src.SRCROWSTATE > 0", nativeQuery = true)
    List<StudentRegistrationCourses> findBySrgId(@Param("srgid") Long srgid);

    @Query(value = "SELECT * FROM ec2.STUDENTREGISTRATIONCOURSES src WHERE src.SRCSRGID = :srgid AND src.SRCSCRID = :scrid AND src.SRCROWSTATE > 0", nativeQuery = true)
    List<StudentRegistrationCourses> findActiveBySrgIdAndScrid(@Param("srgid") Long srgid, @Param("scrid") Long scrid);

    @Modifying
    @Query(value = "UPDATE ec2.STUDENTREGISTRATIONCOURSES SET SRCROWSTATE = 0 WHERE SRCID = :srcid", nativeQuery = true)
    int softDeleteBySrcid(@Param("srcid") Long srcid);
    

    @Query(value = "SELECT COALESCE(MAX(src.srcid), 0) FROM ec2.studentregistrationcourses src", nativeQuery = true)
    Long findMaxSrcId();

    @Query(value = "select src.srctcrid from ec2.studentregistrationcourses src join ec2.coursetypes ct on src.orig_ctpid=ct.ctpid where src.srcsrgid=:srgid and ct.crscat='ELECTIVE'", nativeQuery = true)
    List<Long> findElectivesForStud(@Param("srgid") Long srgid);

    @Query(value = "select * from ec2.studentregistrationcourses src where src.srcsrgid=:srgid and src.srctcrid=:tcrid", nativeQuery = true)
    StudentRegistrationCourses findBySrgidTcrid(@Param("srgid") Long srgid, @Param("tcrid") Long tcrid);

    @Query(value = """
        SELECT DISTINCT
            s.stdid,
            s.stdinstid,
            TRIM(CONCAT(
                COALESCE(s.stdfirstname, ''), ' ',
                COALESCE(s.stdmiddlename, ''), ' ',
                COALESCE(s.stdlastname, '')
            )) AS studentName,
            src.srctype,
            src.srcstatus,
            srg.srgregdate
        FROM ec2.studentregistrationcourses src
        JOIN ec2.studentregistrations srg
          ON srg.srgid = src.srcsrgid
        JOIN ec2.students s
          ON s.stdid = srg.srgstdid
        WHERE src.srctcrid = :tcrid
          AND src.srcrowstate > 0
          AND src.srcstatus = 'ACTIVE'
          AND srg.srgrowstate > 0
          AND s.stdrowstate > 0
        ORDER BY s.stdinstid
        """, nativeQuery = true)
    List<Object[]> findRegisteredStudentsByTcrid(@Param("tcrid") Long tcrid);

    @Query(value = "SELECT SUM(g.obt_credits) AS TOTOBTCREDITS, SUM(tcc.tcccreditpoints) AS TOTCRSCREDITS " +
                "FROM ec2.studentregistrationcourses src " +
                "JOIN ec2.egcrstt1 g ON src.srctcrid = g.tcrid " +
                "JOIN ec2.termcoursecredits tcc ON g.tcrid = tcc.tcctcrid " +
                "JOIN ec2.termcourses tcr ON g.tcrid = tcr.tcrid " +
                "JOIN ec2.courses c ON tcr.tcrcrsid = c.crsid " +
                "WHERE src.srcsrgid = :srgid AND src.srcstatus = 'ACTIVE' " +
                "AND src.srctype <> 'AUDIT' AND g.stud_id = :stdId AND g.obtgr_id <> 8 AND g.obtgr_id <> 10 " +
                "AND c.crsassessmenttype = 'GRADE'", nativeQuery = true)
    List<Object[]> getSpiTotals(@Param("srgid") Long srgid, @Param("stdId") Long stdId);

    @Query(value = "SELECT SUM(tcc.tcccreditpoints) AS ssrcreditsregistered " +
                "FROM ec2.studentregistrationcourses src " +
                "JOIN ec2.egcrstt1 g ON src.srctcrid = g.tcrid " +
                "JOIN ec2.termcoursecredits tcc ON g.tcrid = tcc.tcctcrid " +
                "JOIN ec2.termcourses tcr ON g.tcrid = tcr.tcrid " +
                "WHERE src.srcsrgid = :srgid AND src.srcstatus = 'ACTIVE' " +
                "AND src.srctype <> 'AUDIT' AND g.stud_id = :stdId", nativeQuery = true)
    List<Object[]> getSpiCreditsRegistered(@Param("srgid") Long srgid, @Param("stdId") Long stdId);

    @Query(value = "SELECT SUM(tcc.tcccreditpoints) AS ssrcreditsearned " +
                "FROM ec2.studentregistrationcourses src " +
                "JOIN ec2.egcrstt1 g ON src.srctcrid = g.tcrid " +
                "JOIN ec2.termcoursecredits tcc ON g.tcrid = tcc.tcctcrid " +
                "JOIN ec2.termcourses tcr ON g.tcrid = tcr.tcrid " +
                "WHERE src.srcsrgid = :srgid AND src.srcstatus = 'ACTIVE' " +
                "AND src.srctype <> 'AUDIT' AND g.stud_id = :stdId AND g.obtgr_id <> 8 AND g.obtgr_id <> 5", nativeQuery = true)
    List<Object[]> getSpiCreditsEarned(@Param("srgid") Long srgid, @Param("stdId") Long stdId);

    @Query(value = "SELECT SUM(g.obt_credits) AS CPIGRADEPOINTS, " +
                "SUM(tcc.tcccreditpoints) AS CPIREGISTEREDCREDITS " +
                "FROM ec2.egcrstt1 g " +
                "JOIN ec2.studentregistrationcourses src ON src.srctcrid = g.tcrid " +
                "JOIN ec2.termcoursecredits tcc ON tcc.tcctcrid = g.tcrid " +
                "JOIN ec2.termcourses tc ON tc.tcrid = g.tcrid " +
                "JOIN ec2.courses c ON tc.tcrcrsid = c.crsid " +
                "WHERE g.stud_id = :stdId " +
                "AND src.srctcrid IN (:eligibleIds) " +
                "AND src.srcstatus = 'ACTIVE' " +
                "AND src.srctype <> 'AUDIT' " +
                "AND g.obtgr_id NOT IN (8, 10) " +
                "AND c.crsassessmenttype NOT IN ('RESEARCH', 'PASS/NOT-PASS') " +
                "AND src.srcsrgid IN (" +
                "    SELECT srgid FROM ec2.studentregistrations WHERE srgstdid = g.stud_id" +
                ")", nativeQuery = true)
    List<Object[]> getCpiTotals(@Param("stdId") Long stdId, @Param("eligibleIds") List<Long> eligibleIds);

    @Query(value = "SELECT SUM(tcc.tcccreditpoints) AS ssrcumcreditsregistered " +
                "FROM ec2.egcrstt1 g " +
                "JOIN ec2.studentregistrationcourses src ON src.srctcrid = g.tcrid " +
                "JOIN ec2.termcoursecredits tcc ON tcc.tcctcrid = g.tcrid " +
                "JOIN ec2.termcourses tc ON tc.tcrid = g.tcrid " +
                "JOIN ec2.courses c ON tc.tcrcrsid = c.crsid " +
                "WHERE g.stud_id = :stdId " +
                "AND src.srctcrid IN (:eligibleIds) " +
                "AND src.srcstatus = 'ACTIVE' " +
                "AND src.srctype <> 'AUDIT' " +
                "AND src.srcsrgid IN (" +
                "    SELECT srgid FROM ec2.studentregistrations WHERE srgstdid = g.stud_id" +
                ")", nativeQuery = true)
    List<Object[]> getCpiCreditsRegistered(@Param("stdId") Long stdId, @Param("eligibleIds") List<Long> eligibleIds);

    @Query(value = "SELECT SUM(tcc.tcccreditpoints) AS ssrcumcreditsearned " +
                "FROM ec2.egcrstt1 g " +
                "JOIN ec2.studentregistrationcourses src ON src.srctcrid = g.tcrid " +
                "JOIN ec2.termcoursecredits tcc ON tcc.tcctcrid = g.tcrid " +
                "JOIN ec2.termcourses tc ON tc.tcrid = g.tcrid " +
                "WHERE g.stud_id = :stdId " +
                "AND src.srctcrid IN (:eligibleIds) " +
                "AND src.srcstatus = 'ACTIVE' " +
                "AND src.srctype <> 'AUDIT' " +
                "AND g.obtgr_id NOT IN (8, 5) " +
                "AND src.srcsrgid IN (" +
                "    SELECT srgid FROM ec2.studentregistrations WHERE srgstdid = g.stud_id" +
                ")", nativeQuery = true)
    List<Object[]> getCpiCreditsEarned(@Param("stdId") Long stdId, @Param("eligibleIds") List<Long> eligibleIds);

}

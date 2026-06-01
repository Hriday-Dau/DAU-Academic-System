package com.ecampus.repository;

import com.ecampus.model.SemesterCourses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.*;
import org.springframework.data.repository.query.Param;

@Repository
public interface SemesterCoursesRepository extends JpaRepository<SemesterCourses, Long> {
    // Custom queries can be added here
    @Query(value = "SELECT * FROM ec2.SEMESTERCOURSES sc, ec2.COURSES c, ec2.SEMESTERS s WHERE sc.SCRROWSTATE > 0 AND s.STRROWSTATE > 0 AND c.CRSROWSTATE > 0 AND c.CRSID = sc.SCRCRSID AND s.STRID = sc.SCRSTRID AND sc.SCRELECTIVE = 'N' AND sc.SCRSTRID = :semesterId", nativeQuery = true)
    List<SemesterCourses> getccbysemid(@Param("semesterId") Long semesterId);
    
    // Get CORE courses (screlective='N') with CourseType eagerly loaded for semester registration
    @Query("SELECT sc FROM SemesterCourses sc " +
            "LEFT JOIN FETCH sc.courseType ct " +
            "WHERE sc.scrrowstate > 0 AND sc.scrstrid = :semesterId AND sc.screlective = 'N' " +
            "ORDER BY sc.scrseqno ASC")
    List<SemesterCourses> getCoreCoursesBySemesterId(@Param("semesterId") Long semesterId);

        @Query(value = """
            SELECT sc.scrid,
               sc.scrcrsid,
               sc.scrtcrid,
               c.crscode,
               COALESCE(c.crstitle, c.crsname) AS course_title,
               c.crscreditpoints,
               sc.ctpid,
               ct.ctpcode,
               ct.crscat,
               c.crstype
            FROM ec2.semestercourses sc
            JOIN ec2.courses c ON c.crsid = sc.scrcrsid AND c.crsrowstate > 0
            LEFT JOIN ec2.coursetypes ct ON ct.ctpid = sc.ctpid
            WHERE sc.scrrowstate > 0
              AND sc.scrstrid = :semesterId
              AND sc.screlective = 'N'
                            AND (ct.ctpcode IS NULL OR UPPER(ct.ctpcode) NOT IN ('RI', 'BTP/ITP', 'SI/SRI', 'EXP', 'MJP/ITP', 'PROJECT'))
                            AND UPPER(COALESCE(c.crstype, '')) <> 'PROJECT'
            ORDER BY sc.scrseqno ASC
            """, nativeQuery = true)
        List<Object[]> getCoreCoursesBySemesterRaw(@Param("semesterId") Long semesterId);

        @Query(value = """
                        SELECT sc.scrid,
                                     sc.scrcrsid,
                                     sc.scrtcrid,
                                     c.crscode,
                                     COALESCE(c.crstitle, c.crsname) AS course_title,
                                     c.crscreditpoints,
                                    sc.ctpid,
                                     ct.ctpcode,
                                     ct.crscat,
                                     c.crstype
                        FROM ec2.semestercourses sc
                        JOIN ec2.courses c ON c.crsid = sc.scrcrsid AND c.crsrowstate > 0
                        LEFT JOIN ec2.coursetypes ct ON ct.ctpid = sc.ctpid
                        WHERE sc.scrrowstate > 0
                            AND sc.scrstrid = :semesterId
                            AND sc.screlective = 'N'
                            AND (
                                UPPER(COALESCE(ct.ctpcode, '')) IN ('RI', 'BTP/ITP', 'SI/SRI', 'EXP', 'MJP/ITP', 'PROJECT')
                                OR UPPER(COALESCE(c.crstype, '')) = 'PROJECT'
                            )
                        ORDER BY sc.scrseqno ASC
                        """, nativeQuery = true)
        List<Object[]> getProjectInternshipCoursesBySemesterRaw(@Param("semesterId") Long semesterId);

        @Query(value = """
                                                WITH curr AS (
                                                        SELECT s.strid, s.strbchid, s.strseqno
                                                        FROM ec2.semesters s
                                                        WHERE s.strid = :semesterId
                                                          AND s.strrowstate > 0
                                                ),
                                                failed_transcript AS (
                                                        SELECT DISTINCT
                                                                COALESCE(e.crsid, tc_hist.tcrcrsid) AS match_crsid,
                                                                e.tcrid AS match_tcrid
                                                        FROM ec2.egcrstt1 e
                                                        LEFT JOIN ec2.termcourses tc_hist
                                                            ON tc_hist.tcrid = e.tcrid
                                                           AND tc_hist.tcrrowstate > 0
                                                        LEFT JOIN ec2.eggradm1 g
                                                            ON g.grad_id = e.obtgr_id
                                                        WHERE e.stud_id = :studentId
                                                          AND e.row_st > '0'
                                                          AND (UPPER(COALESCE(g.grad_lt, '')) = 'F' OR e.obtgr_id = 5)
                                                ),
                                                picked AS (
                                                        SELECT sc.scrid,
                                                               sc.scrcrsid,
                                                               sc.scrtcrid,
                                                               c.crscode,
                                                            c.crsname AS course_title,
                                                               c.crscreditpoints,
                                                                                    sc.ctpid,
                                                               ct.ctpcode,
                                                               ct.crscat,
                                                               c.crstype,
                                                               ROW_NUMBER() OVER (
                                                                       PARTITION BY sc.scrcrsid
                                                                       ORDER BY sem.strseqno DESC, sc.scrid DESC
                                                               ) AS rn
                                                        FROM ec2.semestercourses sc
                                                        JOIN ec2.semesters sem
                                                            ON sem.strid = sc.scrstrid
                                                           AND sem.strrowstate > 0
                                                        JOIN curr
                                                            ON sem.strbchid = curr.strbchid
                                                        JOIN ec2.courses c
                                                            ON c.crsid = sc.scrcrsid
                                                           AND c.crsrowstate > 0
                                                        LEFT JOIN ec2.coursetypes ct
                                                            ON ct.ctpid = sc.ctpid
                                                        WHERE sc.scrrowstate > 0
                                                          AND sc.screlective = 'N'
                                                          AND (ct.ctpcode IS NULL OR UPPER(ct.ctpcode) NOT IN ('RI', 'BTP/ITP', 'SI/SRI', 'EXP', 'MJP/ITP', 'PROJECT'))
                                                          AND UPPER(COALESCE(c.crstype, '')) <> 'PROJECT'
                                                          AND sem.strseqno IS NOT NULL
                                                          AND curr.strseqno IS NOT NULL
                                                          AND sem.strseqno < curr.strseqno
                                                          AND MOD(curr.strseqno - sem.strseqno, 2) = 0
                                                          AND (
                                                                  EXISTS (SELECT 1 FROM failed_transcript ft WHERE ft.match_crsid = sc.scrcrsid)
                                                                  OR EXISTS (SELECT 1 FROM failed_transcript ft WHERE ft.match_tcrid = sc.scrtcrid)
                                                          )
                                                )
                                                SELECT picked.scrid,
                                                       picked.scrcrsid,
                                                       picked.scrtcrid,
                                                       picked.crscode,
                                                       picked.course_title,
                                                       picked.crscreditpoints,
                                                       picked.ctpid,
                                                       picked.ctpcode,
                                                       picked.crscat,
                                                       picked.crstype
                                                FROM picked
                                                WHERE picked.rn = 1
                                                ORDER BY picked.crscode ASC
                        """, nativeQuery = true)
        List<Object[]> getBacklogCoursesForStudentBySemesterRaw(
                        @Param("studentId") Long studentId,
                        @Param("semesterId") Long semesterId);

                @Query(value = """
                                                WITH curr AS (
                                                        SELECT s.strid,
                                                               s.strbchid,
                                                               s.strseqno,
                                                               b.scheme_id,
                                                               b.splid,
                                                               sd.spl_grade_imprv_min_cpi
                                                        FROM ec2.semesters s
                                                        JOIN ec2.batches b
                                                          ON b.bchid = s.strbchid
                                                        JOIN ec2.schemedetails sd
                                                          ON sd.scheme_id = b.scheme_id
                                                         AND sd.splid = b.splid
                                                        WHERE s.strid = :semesterId
                                                          AND s.strrowstate > 0
                                                ),
                                                cpi_ctx AS (
                                                        SELECT COALESCE(
                                                                        (
                                                                                SELECT COALESCE(ssr.ssrcpi_numeric, CAST(NULLIF(TRIM(ssr.ssrcpi), '') AS numeric))
                                                                                FROM ec2.studentsemesterresult ssr
                                                                                JOIN ec2.studentregistrations srg
                                                                                    ON srg.srgid = ssr.ssrsrgid
                                                                                   AND srg.srgstdid = :studentId
                                                                                   AND srg.srgrowstate > 0
                                                                                WHERE ssr.ssrrowstate > 0
                                                                                ORDER BY srg.srgstrid DESC, ssr.ssrid DESC
                                                                                LIMIT 1
                                                                        ),
                                                                        0
                                                               ) AS cpi_val
                                                ),
                                                gi_transcript AS (
                                                        SELECT DISTINCT
                                                                COALESCE(e.crsid, tc_hist.tcrcrsid) AS match_crsid,
                                                                e.tcrid AS match_tcrid
                                                        FROM ec2.egcrstt1 e
                                                        LEFT JOIN ec2.termcourses tc_hist
                                                            ON tc_hist.tcrid = e.tcrid
                                                           AND tc_hist.tcrrowstate > 0
                                                        JOIN curr
                                                            ON true
                                                        JOIN ec2.scheme_spl_grade_imprv ssgi
                                                            ON ssgi.scheme_id = curr.scheme_id
                                                           AND ssgi.splid = curr.splid
                                                           AND ssgi.grad_id = e.obtgr_id
                                                        WHERE e.stud_id = :studentId
                                                          AND e.row_st > '0'
                                                ),
                                                picked AS (
                                                        SELECT sc.scrid,
                                                               sc.scrcrsid,
                                                               sc.scrtcrid,
                                                               c.crscode,
                                                                     c.crsname AS course_title,
                                                               c.crscreditpoints,
                                                               sc.ctpid,
                                                               ct.ctpcode,
                                                               ct.crscat,
                                                               c.crstype,
                                                               sem.strseqno,
                                                               ROW_NUMBER() OVER (
                                                                       PARTITION BY sc.scrcrsid
                                                                       ORDER BY sem.strseqno DESC, sc.scrid DESC
                                                               ) AS rn
                                                        FROM ec2.semestercourses sc
                                                        JOIN ec2.semesters sem
                                                            ON sem.strid = sc.scrstrid
                                                           AND sem.strrowstate > 0
                                                        JOIN curr
                                                            ON sem.strbchid = curr.strbchid
                                                        JOIN cpi_ctx
                                                            ON true
                                                        JOIN ec2.courses c
                                                            ON c.crsid = sc.scrcrsid
                                                           AND c.crsrowstate > 0
                                                        LEFT JOIN ec2.coursetypes ct
                                                            ON ct.ctpid = sc.ctpid
                                                        WHERE sc.scrrowstate > 0
                                                          AND sem.strseqno IS NOT NULL
                                                          AND curr.strseqno IS NOT NULL
                                                          AND sem.strseqno < curr.strseqno
                                                          AND MOD(curr.strseqno - sem.strseqno, 2) = 0
                                                          AND sc.screlective = 'N'
                                                          AND (ct.ctpcode IS NULL OR UPPER(ct.ctpcode) NOT IN ('RI', 'BTP/ITP', 'SI/SRI', 'EXP', 'MJP/ITP', 'PROJECT'))
                                                          AND UPPER(COALESCE(c.crstype, '')) <> 'PROJECT'
                                                          AND cpi_ctx.cpi_val < COALESCE(curr.spl_grade_imprv_min_cpi, 0)
                                                          AND (
                                                                  EXISTS (SELECT 1 FROM gi_transcript gt WHERE gt.match_crsid = sc.scrcrsid)
                                                                  OR EXISTS (SELECT 1 FROM gi_transcript gt WHERE gt.match_tcrid = sc.scrtcrid)
                                                          )
                                                )
                                                SELECT picked.scrid,
                                                       picked.scrcrsid,
                                                       picked.scrtcrid,
                                                       picked.crscode,
                                                       picked.course_title,
                                                       picked.crscreditpoints,
                                                           picked.ctpid,
                                                       picked.ctpcode,
                                                       picked.crscat,
                                                       picked.crstype
                                                FROM picked
                                                WHERE picked.rn = 1
                                                ORDER BY picked.crscode ASC
                                                """, nativeQuery = true)
                List<Object[]> getGradeImprovementCoursesForStudentBySemesterRaw(
                                                @Param("studentId") Long studentId,
                                                @Param("semesterId") Long semesterId);

                @Query(value = """
                                                SELECT DISTINCT sc.scrid,
                                                                         sc.scrcrsid,
                                                                         sc.scrtcrid,
                                                                         c.crscode,
                                                                         COALESCE(c.crstitle, c.crsname) AS course_title,
                                                                         c.crscreditpoints,
                                                                        sc.ctpid,
                                                                         ct.ctpcode,
                                                                         ct.crscat,
                                                                         c.crstype
                                                FROM ec2.semestercourses sc
                                                JOIN ec2.semesters s ON s.strid = sc.scrstrid AND s.strrowstate > 0
                                                JOIN ec2.courses c ON c.crsid = sc.scrcrsid AND c.crsrowstate > 0
                                                LEFT JOIN ec2.coursetypes ct ON ct.ctpid = sc.ctpid
                                                WHERE sc.scrrowstate > 0
                                                    AND sc.screlective = 'Y'
                                                        AND s.strtrmid = (
                                                                SELECT s0.strtrmid
                                                                FROM ec2.semesters s0
                                                                WHERE s0.strid = :semesterId
                                                                    AND s0.strrowstate > 0
                                                        )
                                                        AND (
                                                                ct.ctpcode IS NULL
                                                                OR UPPER(ct.ctpcode) NOT IN ('RI', 'BTP/ITP', 'SI/SRI', 'EXP', 'MJP/ITP', 'PROJECT')
                                                        )
                                                        AND UPPER(COALESCE(c.crstype, '')) <> 'PROJECT'
                                                ORDER BY c.crscode ASC, sc.scrid ASC
                                                """, nativeQuery = true)
                List<Object[]> getAuditCoursesForStudentBySemesterRaw(
                                                @Param("semesterId") Long semesterId);

    @Query(value = "SELECT * FROM ec2.SEMESTERCOURSES sc, ec2.COURSES c, ec2.SEMESTERS s WHERE sc.SCRROWSTATE > 0 AND s.STRROWSTATE > 0 AND c.CRSROWSTATE > 0 AND c.CRSID = sc.SCRCRSID AND s.STRID = sc.SCRSTRID AND sc.SCRELECTIVE = 'Y' AND s.STRBCHID = :batchId AND s.STRID < :semesterId AND sc.SCRID NOT IN (SELECT SRC.SRCSCRID FROM ec2.STUDENTREGISTRATIONS SRG, ec2.STUDENTREGISTRATIONCOURSES SRC, ec2.STUDENTSCORES SSC WHERE SRC.SRCROWSTATE > 0 AND SRG.SRGROWSTATE > 0 AND SSC.SSCROWSTATE > 0 AND SRC.SRCSRGID = SRG.SRGID AND SRC.SRCTCRID = SSC.SSCTCRID AND SSC.SSCSTDID = SRG.SRGSTDID AND SRG.SRGSTDID = :studentId)", nativeQuery = true)
    List<SemesterCourses> getBCCourses(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId, @Param("batchId") Long batchId);

    @Query(value = "SELECT * FROM ec2.SEMESTERCOURSES sc, ec2.COURSEGROUPS cg, ec2.SEMESTERS s WHERE sc.SCRROWSTATE > 0 AND s.STRROWSTATE > 0 AND cg.CGPROWSTATE > 0 AND cg.CGPID = sc.SCRCGPID AND s.STRID = sc.SCRSTRID AND sc.SCRELECTIVE = 'Y' AND s.STRBCHID = :batchId AND s.STRID < :semesterId AND sc.SCRID NOT IN (SELECT SRC.SRCSCRID FROM ec2.STUDENTREGISTRATIONS SRG, ec2.STUDENTREGISTRATIONCOURSES SRC, ec2.STUDENTSCORES SSC WHERE SRC.SRCROWSTATE > 0 AND SRG.SRGROWSTATE > 0 AND SSC.SSCROWSTATE > 0 AND SRC.SRCSRGID = SRG.SRGID AND SRC.SRCTCRID = SSC.SSCTCRID AND SSC.SSCSTDID = SRG.SRGSTDID AND SRG.SRGSTDID = :studentId)", nativeQuery = true)
    List<SemesterCourses> getBECourses(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId, @Param("batchId") Long batchId);

//    @Query(value = "SELECT sc.SCRID FROM ec2.SEMESTERCOURSES sc WHERE sc.SCRSTRID = :semesterId AND sc.SCRCRSID = :courseId", nativeQuery = true)
//    Long findScrid(@Param("semesterId") Long semesterId, @Param("courseId") Long courseId);

    // Fetch one semester course entry by SCRID
//    @Query(value = "SELECT * FROM ec2.SEMESTERCOURSES scr " +
//            "WHERE scr.SCRID = :scrid AND scr.SCRROWSTATE > 0",
//            nativeQuery = true)
//    SemesterCourses getByScrid(@Param("scrid") Long scrid);

    // Get all semester courses for a given semester (STRID)
//    @Query(value = "SELECT * FROM ec2.SEMESTERCOURSES scr " +
//            "WHERE scr.SCRSTRID = :semesterId AND scr.SCRROWSTATE > 0 " +
//            "ORDER BY scr.SCRID",
//            nativeQuery = true)
//    List<SemesterCourses> getSemesterCourses(@Param("semesterId") Long semesterId);

    // Get max SCRID for manual ID generation
//    @Query(value = "SELECT MAX(scr.SCRID) FROM ec2.SEMESTERCOURSES scr",
//            nativeQuery = true)
//    Long findMaxSemesterCourseid();

    @Query(value = """
            SELECT CONCAT(t.trmname, ' (', a.ayrname, ')') as term, CONCAT(COALESCE(sd.spldesc, p.prgname), ' ', b.bchname, ' - ', s.strname) AS batchsem, c.crscode AS crscode, COALESCE(c.crsname, ct.ctpname) AS crsname, ct.crscat as crstype,
			CASE 
        	WHEN c.crscreditpoints IS NULL THEN NULL
        	ELSE CONCAT(c.crscreditpoints, ' (', c.crslectures, ' + ', c.crstutorials, ' + ', c.crspracticals, ')')
    		END AS credithours
			FROM ec2.semestercourses AS sc
            JOIN ec2.semesters AS s
            ON sc.scrstrid=s.strid
            JOIN ec2.batches AS b
            ON s.strbchid=b.bchid
            JOIN ec2.programs AS p
            ON b.bchprgid=p.prgid
            LEFT JOIN ec2.schemedetails as sd
            ON b.scheme_id=sd.scheme_id AND b.splid=sd.splid
            JOIN ec2.terms AS t
            ON s.strtrmid=t.trmid
            JOIN ec2.academicyears AS a
            ON t.trmayrid=a.ayrid
            LEFT JOIN ec2.courses AS c
            ON sc.scrcrsid=c.crsid
			LEFT JOIN ec2.coursetypes AS ct
			ON sc.ctpid=ct.ctpid
            ORDER BY t.trmid DESC, b.bchid DESC, sc.scrseqno ASC
            """, nativeQuery = true)
    List<Object[]> getAllSemesterCoursesDetailsRaw();
    
    @Query(value = "SELECT COALESCE(MAX(sc.SCRID), 0) FROM ec2.SEMESTERCOURSES sc", nativeQuery = true)
    Long findMaxSemesterCourseid();
}

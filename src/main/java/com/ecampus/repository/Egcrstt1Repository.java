package com.ecampus.repository;

import java.util.List;

import com.ecampus.model.Egcrstt1;
import com.ecampus.model.Egcrstt1Id;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface Egcrstt1Repository extends JpaRepository<Egcrstt1, Egcrstt1Id> {

    List<Egcrstt1> findByStudIdAndTcridIn(Long studId, List<Long> tcrIds);

    @Query(value = "SELECT COUNT(*) FROM ec2.EGCRSTT1 eg WHERE eg.STUD_ID=:pStdId AND eg.TCRID=:tempTermCourseId AND eg.ROW_ST>0 AND eg.OBTGR_ID = 10", nativeQuery = true)
    Long countPassGradesForAudit(@Param("pStdId") Long pStdId, @Param("tempTermCourseId") Long tempTermCourseId);

    @Query(value = """
            SELECT COUNT(*)
            FROM ec2.egcrstt1 e
            WHERE e.tcrid = :tcrid
              AND e.examtype_id = :examTypeId
              AND e.row_st > '0'
              AND e.obtgr_id IS NOT NULL
            """, nativeQuery = true)
    Long countUploadedGrades(@Param("tcrid") Long tcrid,
            @Param("examTypeId") Long examTypeId);

    @Query(value = """
            SELECT DISTINCT e.tcrid
            FROM ec2.egcrstt1 e
            WHERE e.tcrid IN (:tcrIds)
              AND e.examtype_id = :examTypeId
              AND e.row_st > '0'
              AND e.obtgr_id IS NOT NULL
            """, nativeQuery = true)
    List<Long> findUploadedTcridList(@Param("tcrIds") List<Long> tcrIds,
            @Param("examTypeId") Long examTypeId);

}
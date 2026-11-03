package com.ecampus.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecampus.model.Batches;
import com.ecampus.model.Programs;

@Repository
public interface BatchesRepository extends JpaRepository<Batches, Long> {

//    @Query(value = "SELECT * FROM ec2.BATCHES btc WHERE btc.BCHROWSTATE > 0 AND btc.BCHID = :batchId", nativeQuery = true)
//    Batches getbtchId(@Param("batchId") Long batchId);

    List<Batches> findByPrograms(Programs program);

    @Query("SELECT b FROM Batches b JOIN Programs p ON b.programs.prgid = p.prgid " +
            "WHERE b.bchrowstate > 0 AND p.prgrowstate > 0 ORDER BY p.prgfield1 ASC, b.bchfield1 ASC")
    List<Batches> findAllActiveBatchesOrderedByProgramAndBatchField();

    List<Batches> findByPrograms_PrgidAndBchrowstateGreaterThanOrderByBchfield1Asc(Long prgId, Long rowState);

    @Query("SELECT b FROM Batches b WHERE b.bchprgid = :prgid ORDER BY b.bchname")
    List<Batches> findByPrgId(@Param("prgid") Long prgid);

    @Query(value = "SELECT MAX(b.bchid) FROM ec2.batches b", nativeQuery = true)
    Integer findMaxBatchId();

//    List<Batches> findAllByOrderByBchidDesc();

    @Query(value = "SELECT b.bchid as bchid, a.ayrname as ayrname, COALESCE(sd.spldesc, p.prgname) as spldesc, b.bchname as bchname, b.bchcapacity as bchcapacity, s.effective_from_year as schemeyear FROM ec2.batches AS b JOIN ec2.academicyears AS a ON b.bchcalid=a.ayrid JOIN ec2.programs AS p ON b.bchprgid=p.prgid LEFT JOIN ec2.scheme AS s ON b.scheme_id=s.scheme_id LEFT JOIN ec2.schemedetails AS sd ON b.scheme_id=sd.scheme_id AND b.splid=sd.splid ORDER BY b.bchid DESC", nativeQuery = true)
    List<Object[]> getAllBatchesDetailsRaw();
    
    // Check for duplicate batch
    boolean existsByBchprgidAndSchemeIdAndSplidAndBchcalid(Long bchprgid, Long schemeId, Long splid, Long bchcalid);

    @Query(value = "SELECT DISTINCT bt.bchname FROM ec2.batches bt WHERE bt.bchid IN (SELECT DISTINCT sm.strbchid FROM ec2.semesters sm WHERE sm.strtrmid = :trmid)", nativeQuery = true)
    List<String> findBatchesByTrmId(@Param("trmid") Long trmid);

    @Query(value = "SELECT bt.bchid FROM ec2.batches bt WHERE bt.bchname = :bchname AND bt.scheme_id = :scheme_id AND bt.splid = :splid", nativeQuery = true)
    Long getIdFromBchSchSpl(@Param("bchname") String bchname, @Param("scheme_id") Long scheme_id, @Param("splid") Long splid);

    @Query(value = "SELECT DISTINCT b.bchname FROM ec2.batches b JOIN ec2.schemedetails sd ON b.scheme_id = sd.scheme_id AND b.splid = sd.splid WHERE sd.splname = :splname AND b.bchid IN (SELECT DISTINCT sm.strbchid FROM ec2.semesters sm WHERE sm.strtrmid = :trmid) ORDER BY b.bchname;", nativeQuery = true)
    List<String> findBatchBySplnTrm(@Param("splname") String splname, @Param("trmid") Long trmid);

    @Query(value = "SELECT COALESCE(MAX(b.bchid), 0) + 1 FROM ec2.batches b", nativeQuery = true)
    Long findNextBatchId();

    @Query(value = """
        SELECT b.bchid, b.bchname, p.prgname, COALESCE(sd.splname, p.prgdesc, 'General') as splname,
               b.bchcapacity, b.bchinstcode,
               (SELECT COUNT(1) FROM ec2.students s WHERE s.stdbchid = b.bchid AND s.stdrowstate > 0) AS student_count
        FROM ec2.batches b
        JOIN ec2.programs p ON b.bchprgid = p.prgid
        LEFT JOIN ec2.schemedetails sd ON b.scheme_id = sd.scheme_id AND b.splid = sd.splid
        WHERE b.bchrowstate >= 0
        ORDER BY b.bchid DESC
    """, nativeQuery = true)
    List<Object[]> getAllBatchesMasterDetails();

    @Query(value = """
        SELECT b.bchid, b.bchname, p.prgname, COALESCE(sd.splname, p.prgdesc, 'General') as splname,
               b.bchcapacity, b.bchinstcode,
               (SELECT COUNT(1) FROM ec2.students s WHERE s.stdbchid = b.bchid AND s.stdrowstate > 0) AS student_count
        FROM ec2.batches b
        JOIN ec2.programs p ON b.bchprgid = p.prgid
        LEFT JOIN ec2.schemedetails sd ON b.scheme_id = sd.scheme_id AND b.splid = sd.splid
        WHERE b.bchrowstate >= 0 AND (
            LOWER(b.bchname) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.prgname) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(sd.splname, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY b.bchid DESC
    """, nativeQuery = true)
    List<Object[]> searchBatchesMasterDetails(@Param("q") String q);

    boolean existsByBchprgidAndBchname(Long bchprgid, String bchname);

}

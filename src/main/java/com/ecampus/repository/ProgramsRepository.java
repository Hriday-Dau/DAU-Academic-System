package com.ecampus.repository;

import com.ecampus.model.Programs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramsRepository extends JpaRepository<Programs, Long> {

    @Query(value = "SELECT * FROM ec2.PROGRAMS prg WHERE prg.PRGROWSTATE > 0 AND prg.PRGID = :programId", nativeQuery = true)
    Programs getprgId(@Param("programId") Long programId);

    List<Programs> findByPrgrowstateGreaterThanOrderByPrgfield1Asc(short prgrowstate);

    @Query(value = "SELECT prg_level FROM ec2.programs WHERE prgid = (SELECT program_id FROM ec2.scheme WHERE scheme_id = :scheme_id);", nativeQuery = true)
    String getUGPGByScheme(@Param("scheme_id") Long scheme_id);

    // 
    @Query("SELECT MAX(p.prgid) FROM Programs p")
    Long findMaxPrgid();

    // Fetch only active programs (rowstate > 0 and <= 900)
    @Query("SELECT p FROM Programs p WHERE p.prgrowstate > 0 AND p.prgrowstate <= 900 ORDER BY p.prgid ASC")
    List<Programs> findActivePrograms();

    // Fetch only archived programs (rowstate > 900)
    @Query("SELECT p FROM Programs p WHERE p.prgrowstate > 900 ORDER BY p.prgid ASC")
    List<Programs> findArchivedPrograms();

    // Check if an institute code (prginstcode) already exists
    @Query("SELECT COUNT(p) > 0 FROM Programs p WHERE p.prginstcode = :instCode")
    boolean existsByInstituteCode(@Param("instCode") String instCode);
    
}
package com.ecampus.repository;

import com.ecampus.model.GradeModRequestDetails;
import com.ecampus.model.GradeModRequestDetailsId;
import com.ecampus.dto.GradeModDetailDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeModRequestDetailsRepository extends JpaRepository<GradeModRequestDetails, GradeModRequestDetailsId> {

    // Updated JPQL Query:
    @Query("SELECT s.stdinstid AS studentId, " + // Display Institute ID
           "CONCAT(s.stdfirstname, ' ', s.stdlastname) AS studentName, " + // Full Name
           "g1.gradLt AS presentGradeLetter, " +
           "g2.gradLt AS newGradeLetter, " +
           "d.gmdChangeDesc AS changeDescription " +
           "FROM GradeModRequestDetails d " +
           "JOIN Students s ON d.gmdStdId = s.stdid " +
           "JOIN Eggradm1 g1 ON d.gmdPresentGrade = g1.gradId " +
           "JOIN Eggradm1 g2 ON d.gmdNewGrade = g2.gradId " +
           "WHERE d.gmdId = :requestId")
    List<GradeModDetailDTO> findDetailsByRequestId(@Param("requestId") Long requestId);

    // all students included in a specific request
    
    List<GradeModRequestDetails> findByGmdId(Long gmdId);

    //incorrect method name 'findByGmdReqId' has been removed because entity property is 'gmdId'
}


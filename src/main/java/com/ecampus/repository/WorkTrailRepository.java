package com.ecampus.repository;

import com.ecampus.model.WorkTrail;
import com.ecampus.model.WorkTrailId; // Importing the ID class
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkTrailRepository extends JpaRepository<WorkTrail, WorkTrailId> {

    // entire history of a specific work request
    List<WorkTrail> findByWorkIdOrderByResponseDateDesc(Long workId);
    
    //Checking if any specific node action has already been taken (prevent double-processing)
    boolean existsByWorkIdAndNodeNumberAndIterationNumberAndResponseCode(
            Long workId, Integer nodeNumber, Integer iterationNumber, Integer responseCode);
}
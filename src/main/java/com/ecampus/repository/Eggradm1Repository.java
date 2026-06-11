package com.ecampus.repository;

import com.ecampus.model.Eggradm1;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Eggradm1Repository extends JpaRepository<Eggradm1, Long> {

    @Query("SELECT g FROM Eggradm1 g WHERE g.rowSt > 0 ORDER BY g.gradId")
    List<Eggradm1> findActiveGrades();

    @Query("SELECT g.gradId FROM Eggradm1 g WHERE UPPER(TRIM(g.gradLt)) = UPPER(TRIM(:gradeValue))")
    Long findGradeIdByValue(@Param("gradeValue") String gradeValue);

    @Query("SELECT g.gradPt FROM Eggradm1 g WHERE UPPER(TRIM(g.gradLt)) = UPPER(TRIM(:gradeValue))")
    BigDecimal findGradePointByValue(@Param("gradeValue") String gradeValue);

    @Query("SELECT g.gradLt FROM Eggradm1 g WHERE g.gradId = :gradeId")
    String getGrade(@Param("gradeId") Long gradeId);
}

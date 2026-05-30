package com.ecampus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeModAdminSummaryDTO {
    private Long id;
    private String facultyUnivId; // univid from Users
    private String facultyFullName; // ufullname from Users
    private String courseInfo; //Course + Term + Year format
    private LocalDateTime date;
    private String status;
    private String facultyRemarks;
    private String deanRemarks; //Node-1 feedback
}
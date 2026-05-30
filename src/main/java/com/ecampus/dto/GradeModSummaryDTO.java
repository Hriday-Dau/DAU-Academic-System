package com.ecampus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeModSummaryDTO {
    private Long id;
    private String courseCode;
    private String courseName;
    private String fullCourseInfo;
    private String approverRemarks; // Dean/Registrar feedback
    private String status;
    private LocalDateTime date;
    
    // A helper method to dsiplay  {"Course Code" - "Course Name"}
    public String getCourseInfo() {
        return courseCode + " - " + courseName;
    }
}


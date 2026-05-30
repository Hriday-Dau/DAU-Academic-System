package com.ecampus.dto;

// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// public class GradeModDetailDTO {
//     private Long studentId;
//     private String studentName;
//     private String presentGradeLetter;
//     private String newGradeLetter;
//     private String changeDescription;
// }

public interface GradeModDetailDTO {
    Long getStudentId();
    String getStudentName();
    String getPresentGradeLetter();
    String getNewGradeLetter();
    String getChangeDescription();
}
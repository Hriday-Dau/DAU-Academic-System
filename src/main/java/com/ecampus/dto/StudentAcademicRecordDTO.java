package com.ecampus.dto;

public interface StudentAcademicRecordDTO {
    Long getSemesterId();
    String getSemesterName();
    String getCourseCode();
    String getCourseTitle();
    String getCourseType();
    Double getCredits();
    String getGrade();
}
package com.ecampus.dto;

import java.math.BigDecimal;

public record StudentResultCourseProjection(
        String courseTitle,
        String courseCode,
        String courseType,
        BigDecimal credits,
        BigDecimal lectures,
        BigDecimal tutorials,
        BigDecimal practicals,
        String gradeLetter,
        BigDecimal gradePointValue,
        String remarks) {

    public String getCourseTitle() { return courseTitle; }
    public String getCourseCode() { return courseCode; }
    public String getCourseType() { return courseType; }
    public BigDecimal getCredits() { return credits; }
    public BigDecimal getLectures() { return lectures; }
    public BigDecimal getTutorials() { return tutorials; }
    public BigDecimal getPracticals() { return practicals; }
    public String getGradeLetter() { return gradeLetter; }
    public BigDecimal getGradePointValue() { return gradePointValue; }
    public String getRemarks() { return remarks; }
}

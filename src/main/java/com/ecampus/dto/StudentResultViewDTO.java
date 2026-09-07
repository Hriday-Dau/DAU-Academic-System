package com.ecampus.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentResultViewDTO {

    private boolean available;
    private String message = "No result is available.";
    private Long selectedSemesterId;
    private String studentName;
    private String studentInstId;
    private String programName;
    private String batchName;
    private String semesterName;
    private String registrationDate;
    private List<SemesterOption> semesters = new ArrayList<>();
    private List<CourseRow> courses = new ArrayList<>();
    private Performance semesterPerformance = new Performance();
    private Performance cumulativePerformance = new Performance();

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getSelectedSemesterId() { return selectedSemesterId; }
    public void setSelectedSemesterId(Long selectedSemesterId) { this.selectedSemesterId = selectedSemesterId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentInstId() { return studentInstId; }
    public void setStudentInstId(String studentInstId) { this.studentInstId = studentInstId; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }

    public List<SemesterOption> getSemesters() { return semesters; }
    public void setSemesters(List<SemesterOption> semesters) { this.semesters = semesters; }

    public boolean hasSemesters() { return semesters != null && !semesters.isEmpty(); }

    public List<CourseRow> getCourses() { return courses; }
    public void setCourses(List<CourseRow> courses) { this.courses = courses; }

    public Performance getSemesterPerformance() { return semesterPerformance; }
    public void setSemesterPerformance(Performance semesterPerformance) { this.semesterPerformance = semesterPerformance; }

    public Performance getCumulativePerformance() { return cumulativePerformance; }
    public void setCumulativePerformance(Performance cumulativePerformance) { this.cumulativePerformance = cumulativePerformance; }

    public static class SemesterOption {
        private Long semesterId;
        private String label;

        public SemesterOption(Long semesterId, String label) {
            this.semesterId = semesterId;
            this.label = label;
        }

        public Long getSemesterId() { return semesterId; }
        public String getLabel() { return label; }
    }

    public static class CourseRow {
        private String courseTitle;
        private String courseCode;
        private String courseType;
        private String creditHours;
        private String grade;
        private String gradePoints;
        private String remarks;

        public CourseRow(String courseTitle, String courseCode, String courseType, String creditHours,
                         String grade, String gradePoints, String remarks) {
            this.courseTitle = courseTitle;
            this.courseCode = courseCode;
            this.courseType = courseType;
            this.creditHours = creditHours;
            this.grade = grade;
            this.gradePoints = gradePoints;
            this.remarks = remarks;
        }

        public String getCourseTitle() { return courseTitle; }
        public String getCourseCode() { return courseCode; }
        public String getCourseType() { return courseType; }
        public String getCreditHours() { return creditHours; }
        public String getGrade() { return grade; }
        public String getGradePoints() { return gradePoints; }
        public String getRemarks() { return remarks; }
    }

    public static class Performance {
        private String creditsRegistered = "0.00";
        private String creditsEarned = "0.00";
        private String gradePointsEarned = "0.00";
        private String index = "--";

        public Performance() {
        }

        public Performance(String creditsRegistered, String creditsEarned,
                           String gradePointsEarned, String index) {
            this.creditsRegistered = creditsRegistered;
            this.creditsEarned = creditsEarned;
            this.gradePointsEarned = gradePointsEarned;
            this.index = index;
        }

        public String getCreditsRegistered() { return creditsRegistered; }
        public String getCreditsEarned() { return creditsEarned; }
        public String getGradePointsEarned() { return gradePointsEarned; }
        public String getIndex() { return index; }
    }
}

package com.ecampus.model;

public class GradeChangeStatusDTO {

    private Long gmdid;
    private String studentInstituteId;
    private String presentGradeLetter;
    private String newGradeLetter;
    private String remarks;
    private String statusSubmitted;
    private String statusDean;
    private String statusRegistrar;

    public Long getGmdid() {
        return gmdid;
    }

    public void setGmdid(Long gmdid) {
        this.gmdid = gmdid;
    }

    public String getStudentInstituteId() {
        return studentInstituteId;
    }

    public void setStudentInstituteId(String studentInstituteId) {
        this.studentInstituteId = studentInstituteId;
    }

    public String getPresentGradeLetter() {
        return presentGradeLetter;
    }

    public void setPresentGradeLetter(String presentGradeLetter) {
        this.presentGradeLetter = presentGradeLetter;
    }

    public String getNewGradeLetter() {
        return newGradeLetter;
    }

    public void setNewGradeLetter(String newGradeLetter) {
        this.newGradeLetter = newGradeLetter;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatusSubmitted() {
        return statusSubmitted;
    }

    public void setStatusSubmitted(String statusSubmitted) {
        this.statusSubmitted = statusSubmitted;
    }

    public String getStatusDean() {
        return statusDean;
    }

    public void setStatusDean(String statusDean) {
        this.statusDean = statusDean;
    }

    public String getStatusRegistrar() {
        return statusRegistrar;
    }

    public void setStatusRegistrar(String statusRegistrar) {
        this.statusRegistrar = statusRegistrar;
    }
}

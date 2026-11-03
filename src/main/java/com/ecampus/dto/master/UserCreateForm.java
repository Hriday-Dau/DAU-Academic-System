package com.ecampus.dto.master;

public class UserCreateForm {

    private Long uid;
    private String univId;
    private String uname;
    private String ufullname;
    private String urole;
    private String uemail;

    public UserCreateForm() {}

    public UserCreateForm(Long uid, String univId, String uname, String ufullname, String urole, String uemail) {
        this.uid = uid;
        this.univId = univId;
        this.uname = uname;
        this.ufullname = ufullname;
        this.urole = urole;
        this.uemail = uemail;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getUnivId() {
        return univId;
    }

    public void setUnivId(String univId) {
        this.univId = univId;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getUfullname() {
        return ufullname;
    }

    public void setUfullname(String ufullname) {
        this.ufullname = ufullname;
    }

    public String getUrole() {
        return urole;
    }

    public void setUrole(String urole) {
        this.urole = urole;
    }

    public String getUemail() {
        return uemail;
    }

    public void setUemail(String uemail) {
        this.uemail = uemail;
    }
}

